package pe.com.movimientos.facturacionsunat.service;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.com.movimientos.facturacionsunat.dto.EnvioComprobanteRequest;
import pe.com.movimientos.facturacionsunat.dto.EnvioComprobanteResponse;
import pe.com.movimientos.facturacionsunat.dto.ConsultaTicketResponse;
import pe.gob.sunat.service.factura.BillService;
import pe.gob.sunat.service.factura.StatusResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class SunatService {

    private final BillService billService;

    /**
     * Envía una factura o boleta a SUNAT
     * @param request Datos del comprobante (XML en base64 o texto)
     * @return Respuesta de SUNAT con el CDR
     */
    public EnvioComprobanteResponse enviarComprobante(EnvioComprobanteRequest request) {
        try {
            log.info("Enviando comprobante: {}", request.getNombreArchivo());

            // Preparar el archivo ZIP
            byte[] zipContent = crearZip(request.getNombreArchivo(), request.getContenidoXml());

            // Crear DataHandler para el contenido
            DataSource dataSource = new ByteArrayDataSource(zipContent, "application/zip");
            DataHandler dataHandler = new DataHandler(dataSource);

            // Nombre del archivo ZIP (sin extensión .xml)
            String nombreZip = request.getNombreArchivo().replace(".xml", ".zip");

            // Enviar a SUNAT
            byte[] respuesta = billService.sendBill(nombreZip, dataHandler, null);

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
     * Envía un resumen diario o comunicación de baja a SUNAT
     * @param request Datos del resumen
     * @return Ticket para consulta posterior
     */
    public EnvioComprobanteResponse enviarResumen(EnvioComprobanteRequest request) {
        try {
            log.info("Enviando resumen: {}", request.getNombreArchivo());

            byte[] zipContent = crearZip(request.getNombreArchivo(), request.getContenidoXml());

            DataSource dataSource = new ByteArrayDataSource(zipContent, "application/zip");
            DataHandler dataHandler = new DataHandler(dataSource);

            String nombreZip = request.getNombreArchivo().replace(".xml", ".zip");

            // SendSummary retorna un ticket
            String ticket = billService.sendSummary(nombreZip, dataHandler, null);

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
     * Consulta el estado de un ticket (para resúmenes y bajas)
     * @param ticket Ticket obtenido al enviar resumen
     * @return Estado del procesamiento
     */
    public ConsultaTicketResponse consultarTicket(String ticket) {
        try {
            log.info("Consultando ticket: {}", ticket);

            StatusResponse status = billService.getStatus(ticket);

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

    /**
     * Envía un paquete de comprobantes a SUNAT
     */
    public EnvioComprobanteResponse enviarPaquete(EnvioComprobanteRequest request) {
        try {
            log.info("Enviando paquete: {}", request.getNombreArchivo());

            byte[] zipContent = crearZip(request.getNombreArchivo(), request.getContenidoXml());

            DataSource dataSource = new ByteArrayDataSource(zipContent, "application/zip");
            DataHandler dataHandler = new DataHandler(dataSource);

            String nombreZip = request.getNombreArchivo().replace(".xml", ".zip");

            String ticket = billService.sendPack(nombreZip, dataHandler, null);

            return EnvioComprobanteResponse.builder()
                    .exitoso(true)
                    .ticket(ticket)
                    .mensaje("Paquete enviado. Ticket: " + ticket)
                    .build();

        } catch (Exception e) {
            log.error("Error al enviar paquete: {}", e.getMessage(), e);
            return EnvioComprobanteResponse.builder()
                    .exitoso(false)
                    .mensaje("Error: " + e.getMessage())
                    .codigoError(extraerCodigoError(e.getMessage()))
                    .build();
        }
    }

    private byte[] crearZip(String nombreArchivo, String contenidoXml) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(nombreArchivo);
            zos.putNextEntry(entry);

            byte[] xmlBytes;
            // Verificar si el contenido ya está en base64
            try {
                xmlBytes = Base64.getDecoder().decode(contenidoXml);
            } catch (IllegalArgumentException e) {
                // No es base64, usar como texto plano
                xmlBytes = contenidoXml.getBytes("UTF-8");
            }

            zos.write(xmlBytes);
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
