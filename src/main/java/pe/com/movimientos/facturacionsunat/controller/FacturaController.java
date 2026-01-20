package pe.com.movimientos.facturacionsunat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.movimientos.facturacionsunat.dto.*;
import pe.com.movimientos.facturacionsunat.service.FacturacionService;
import pe.com.movimientos.facturacionsunat.service.SunatService;

@RestController
@RequestMapping("/api/sunat")
@RequiredArgsConstructor
@Slf4j
public class FacturaController {

    private final SunatService sunatService;
    private final FacturacionService facturacionService;

    /**
     * ENDPOINT PRINCIPAL - Emitir factura/boleta completa
     * Recibe los datos, genera XML, firma y envia a SUNAT
     * POST /api/sunat/factura/emitir
     */
    @PostMapping("/factura/emitir")
    public ResponseEntity<EnvioComprobanteResponse> emitirFactura(
            @RequestBody ComprobanteDto comprobante) {
        log.info("Emitiendo comprobante: {}-{}", comprobante.getSerie(), comprobante.getCorrelativo());
        EnvioComprobanteResponse response = facturacionService.emitirComprobante(comprobante);
        return response.isExitoso()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Preview del XML generado (sin enviar a SUNAT)
     * POST /api/sunat/factura/preview
     */
    @PostMapping(value = "/factura/preview", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> previewXml(@RequestBody ComprobanteDto comprobante) {
        try {
            String xml = facturacionService.generarXmlPreview(comprobante);
            return ResponseEntity.ok(xml);
        } catch (Exception e) {
            log.error("Error generando preview: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Genera XML firmado (sin enviar a SUNAT)
     * POST /api/sunat/factura/firmar
     */
    @PostMapping(value = "/factura/firmar", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> firmarXml(@RequestBody ComprobanteDto comprobante) {
        try {
            String xmlFirmado = facturacionService.generarXmlFirmado(comprobante);
            return ResponseEntity.ok(xmlFirmado);
        } catch (Exception e) {
            log.error("Error firmando XML: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ==================== ENDPOINTS ORIGINALES ====================

    /**
     * Envia un XML ya preparado a SUNAT
     * POST /api/sunat/comprobante
     */
    @PostMapping("/comprobante")
    public ResponseEntity<EnvioComprobanteResponse> enviarComprobante(
            @RequestBody EnvioComprobanteRequest request) {
        log.info("Recibida solicitud para enviar comprobante: {}", request.getNombreArchivo());
        EnvioComprobanteResponse response = sunatService.enviarComprobante(request);
        return response.isExitoso()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Envia un resumen diario o comunicacion de baja a SUNAT
     * POST /api/sunat/resumen
     */
    @PostMapping("/resumen")
    public ResponseEntity<EnvioComprobanteResponse> enviarResumen(
            @RequestBody EnvioComprobanteRequest request) {
        log.info("Recibida solicitud para enviar resumen: {}", request.getNombreArchivo());
        EnvioComprobanteResponse response = sunatService.enviarResumen(request);
        return response.isExitoso()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Envia un paquete de comprobantes a SUNAT
     * POST /api/sunat/paquete
     */
    @PostMapping("/paquete")
    public ResponseEntity<EnvioComprobanteResponse> enviarPaquete(
            @RequestBody EnvioComprobanteRequest request) {
        log.info("Recibida solicitud para enviar paquete: {}", request.getNombreArchivo());
        EnvioComprobanteResponse response = sunatService.enviarPaquete(request);
        return response.isExitoso()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Consulta el estado de un ticket
     * GET /api/sunat/ticket/{ticket}
     */
    @GetMapping("/ticket/{ticket}")
    public ResponseEntity<ConsultaTicketResponse> consultarTicket(
            @PathVariable String ticket) {
        log.info("Consultando ticket: {}", ticket);
        ConsultaTicketResponse response = sunatService.consultarTicket(ticket);
        return response.isExitoso()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Health check del servicio
     * GET /api/sunat/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Servicio de facturacion SUNAT activo");
    }
}
