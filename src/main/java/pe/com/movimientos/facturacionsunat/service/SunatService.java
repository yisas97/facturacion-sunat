package pe.com.movimientos.facturacionsunat.service;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.BindingProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.com.movimientos.facturacionsunat.dto.ConsultaTicketResponse;
import pe.com.movimientos.facturacionsunat.dto.EnvioComprobanteResponse;
import pe.com.movimientos.facturacionsunat.entity.Emisor;
import pe.gob.sunat.service.factura.BillService;
import pe.gob.sunat.service.factura.StatusResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class SunatService {

    @Value("${sunat.endpoint.beta}")
    private String endpointBeta;

    @Value("${sunat.endpoint.produccion}")
    private String endpointProduccion;

    /**
     * Crea un cliente SOAP configurado para el emisor
     */
    private BillService crearCliente(Emisor emisor) {
        String endpoint = "PRODUCCION".equalsIgnoreCase(emisor.getAmbiente())
                ? endpointProduccion
                : endpointBeta;

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(BillService.class);
        factory.setAddress(endpoint);

        BillService port = (BillService) factory.create();

        BindingProvider bindingProvider = (BindingProvider) port;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();

        // Usuario SOL: RUC + Usuario
        String username = emisor.getRuc() + emisor.getUsuarioSol();
        requestContext.put(BindingProvider.USERNAME_PROPERTY, username);
        requestContext.put(BindingProvider.PASSWORD_PROPERTY, emisor.getClaveSol());

        log.info("Cliente SOAP creado para emisor: {} - Ambiente: {}", emisor.getRuc(), emisor.getAmbiente());

        return port;
    }

    /**
     * Envia una factura o boleta a SUNAT
     */
    public EnvioComprobanteResponse enviarComprobante(String nombreArchivo, String xmlFirmado, Emisor emisor) {
        try {
            log.info("Enviando comprobante: {} para emisor: {}", nombreArchivo, emisor.getRuc());

            BillService cliente = crearCliente(emisor);

            // Preparar el archivo ZIP
            byte[] zipContent = crearZip(nombreArchivo, xmlFirmado);

            // Crear DataHandler para el contenido
            DataSource dataSource = new ByteArrayDataSource(zipContent, "application/zip");
            DataHandler dataHandler = new DataHandler(dataSource);

            // Nombre del archivo ZIP
            String nombreZip = nombreArchivo.replace(".xml", ".zip");

            // Enviar a SUNAT
            byte[] respuesta = cliente.sendBill(nombreZip, dataHandler, null);

            // Procesar respuesta (CDR)
            String cdrBase64 = Base64.getEncoder().encodeToString(respuesta);
            String cdrXml = extraerXmlDeZip(respuesta);

            return EnvioComprobanteResponse.builder()
                    .exitoso(true)
                    .cdrBase64(cdrBase64)
                    .cdrXml(cdrXml)
                    .mensaje("Comprobante enviado exitosamente")
                    .build();

        } catch (Exception e) {
            log.error("Error al enviar comprobante: {}", e.getMessage(), e);
            return EnvioComprobanteResponse.builder()
                    .exitoso(false)
                    .mensaje("Error: " + e.getMessage())
                    .codigoError(extraerCodigoError(e.getMessage()))
                    .build();
        }
    }

    /**
     * Envia un resumen diario o comunicacion de baja a SUNAT
     */
    public EnvioComprobanteResponse enviarResumen(String nombreArchivo, String xmlFirmado, Emisor emisor) {
        try {
            log.info("Enviando resumen: {} para emisor: {}", nombreArchivo, emisor.getRuc());

            BillService cliente = crearCliente(emisor);

            byte[] zipContent = crearZip(nombreArchivo, xmlFirmado);
            DataSource dataSource = new ByteArrayDataSource(zipContent, "application/zip");
            DataHandler dataHandler = new DataHandler(dataSource);

            String nombreZip = nombreArchivo.replace(".xml", ".zip");

            // SendSummary retorna un ticket
            String ticket = cliente.sendSummary(nombreZip, dataHandler, null);

            return EnvioComprobanteResponse.builder()
                    .exitoso(true)
                    .ticket(ticket)
                    .mensaje("Resumen enviado. Ticket: " + ticket)
                    .build();

        } catch (Exception e) {
            log.error("Error al enviar resumen: {}", e.getMessage(), e);
            return EnvioComprobanteResponse.builder()
                    .exitoso(false)
                    .mensaje("Error: " + e.getMessage())
                    .codigoError(extraerCodigoError(e.getMessage()))
                    .build();
        }
    }

    /**
     * Consulta el estado de un ticket
     */
    public ConsultaTicketResponse consultarTicket(String ticket, Emisor emisor) {
        try {
            log.info("Consultando ticket: {} para emisor: {}", ticket, emisor.getRuc());

            BillService cliente = crearCliente(emisor);

            StatusResponse status = cliente.getStatus(ticket);

            String cdrBase64 = null;
            String cdrXml = null;

            if (status.getContent() != null && status.getContent().length > 0) {
                cdrBase64 = Base64.getEncoder().encodeToString(status.getContent());
                cdrXml = extraerXmlDeZip(status.getContent());
            }

            return ConsultaTicketResponse.builder()
                    .exitoso(true)
                    .codigoEstado(status.getStatusCode())
                    .cdrBase64(cdrBase64)
                    .cdrXml(cdrXml)
                    .mensaje(obtenerMensajeEstado(status.getStatusCode()))
                    .build();

        } catch (Exception e) {
            log.error("Error al consultar ticket: {}", e.getMessage(), e);
            return ConsultaTicketResponse.builder()
                    .exitoso(false)
                    .mensaje("Error: " + e.getMessage())
                    .build();
        }
    }

    private byte[] crearZip(String nombreArchivo, String contenidoXml) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(nombreArchivo);
            zos.putNextEntry(entry);
            zos.write(contenidoXml.getBytes("UTF-8"));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private String extraerXmlDeZip(byte[] zipContent) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipContent))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xml")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    return baos.toString("UTF-8");
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer XML del ZIP: {}", e.getMessage());
        }
        return null;
    }

    private String extraerCodigoError(String mensaje) {
        if (mensaje != null && mensaje.contains("Code:")) {
            int start = mensaje.indexOf("Code:") + 5;
            int end = mensaje.indexOf(" ", start);
            if (end > start) {
                return mensaje.substring(start, end).trim();
            }
        }
        return null;
    }

    private String obtenerMensajeEstado(String codigo) {
        return switch (codigo) {
            case "0" -> "Procesado correctamente";
            case "98" -> "En proceso";
            case "99" -> "Procesado con errores";
            default -> "Estado: " + codigo;
        };
    }
}
