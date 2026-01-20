package pe.com.movimientos.facturacionsunat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.com.movimientos.facturacionsunat.dto.ComprobanteDto;
import pe.com.movimientos.facturacionsunat.dto.EnvioComprobanteRequest;
import pe.com.movimientos.facturacionsunat.dto.EnvioComprobanteResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacturacionService {

    private final XmlGeneratorService xmlGeneratorService;
    private final FirmaDigitalService firmaDigitalService;
    private final SunatService sunatService;

    /**
     * Proceso completo: Genera XML, firma y envia a SUNAT
     */
    public EnvioComprobanteResponse emitirComprobante(ComprobanteDto comprobante) {
        try {
            log.info("Iniciando emision de comprobante {}-{}", comprobante.getSerie(), comprobante.getCorrelativo());

            // 1. Generar XML
            log.info("Generando XML...");
            String xmlSinFirma = xmlGeneratorService.generarXmlFactura(comprobante);
            log.debug("XML generado:\n{}", xmlSinFirma);

            // 2. Firmar XML
            String xmlFirmado;
            if (firmaDigitalService.isCertificadoDisponible()) {
                log.info("Firmando XML...");
                xmlFirmado = firmaDigitalService.firmarXml(xmlSinFirma);
                log.debug("XML firmado:\n{}", xmlFirmado);
            } else {
                log.warn("Certificado no disponible. Enviando sin firma (solo para pruebas).");
                xmlFirmado = xmlSinFirma;
            }

            // 3. Obtener nombre de archivo
            String nombreArchivo = xmlGeneratorService.getNombreArchivo(comprobante);
            log.info("Nombre de archivo: {}", nombreArchivo);

            // 4. Enviar a SUNAT
            log.info("Enviando a SUNAT...");
            EnvioComprobanteRequest request = EnvioComprobanteRequest.builder()
                    .nombreArchivo(nombreArchivo)
                    .contenidoXml(xmlFirmado)
                    .build();

            EnvioComprobanteResponse response = sunatService.enviarComprobante(request);

            if (response.isExitoso()) {
                log.info("Comprobante emitido exitosamente: {}", nombreArchivo);
            } else {
                log.error("Error al emitir comprobante: {}", response.getMensaje());
            }

            return response;

        } catch (Exception e) {
            log.error("Error en proceso de emision: {}", e.getMessage(), e);
            return EnvioComprobanteResponse.builder()
                    .exitoso(false)
                    .mensaje("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Solo genera el XML (para pruebas)
     */
    public String generarXmlPreview(ComprobanteDto comprobante) throws Exception {
        return xmlGeneratorService.generarXmlFactura(comprobante);
    }

    /**
     * Genera y firma el XML (para pruebas)
     */
    public String generarXmlFirmado(ComprobanteDto comprobante) throws Exception {
        String xmlSinFirma = xmlGeneratorService.generarXmlFactura(comprobante);
        if (firmaDigitalService.isCertificadoDisponible()) {
            return firmaDigitalService.firmarXml(xmlSinFirma);
        }
        return xmlSinFirma;
    }
}
