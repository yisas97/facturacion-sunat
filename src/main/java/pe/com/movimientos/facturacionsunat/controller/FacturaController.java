package pe.com.movimientos.facturacionsunat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.movimientos.facturacionsunat.dto.*;
import pe.com.movimientos.facturacionsunat.entity.Comprobante;
import pe.com.movimientos.facturacionsunat.entity.Emisor;
import pe.com.movimientos.facturacionsunat.service.EmisorService;
import pe.com.movimientos.facturacionsunat.service.FacturacionService;
import pe.com.movimientos.facturacionsunat.service.SunatService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sunat")
@RequiredArgsConstructor
@Slf4j
public class FacturaController {

    private final SunatService sunatService;
    private final FacturacionService facturacionService;
    private final EmisorService emisorService;

    /**
     * ENDPOINT PRINCIPAL - Emitir factura/boleta completa
     * Recibe los datos, genera XML, firma y envia a SUNAT
     * POST /api/sunat/factura/emitir
     */
    @PostMapping("/factura/emitir")
    public ResponseEntity<EnvioComprobanteResponse> emitirFactura(
            @Valid @RequestBody ComprobanteDto comprobante) {
        log.info("Emitiendo comprobante: {}-{} para emisor ID: {}",
                comprobante.getSerie(), comprobante.getCorrelativo(), comprobante.getEmisorId());
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
    public ResponseEntity<String> previewXml(@Valid @RequestBody ComprobanteDto comprobante) {
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
    public ResponseEntity<String> firmarXml(@Valid @RequestBody ComprobanteDto comprobante) {
        try {
            String xmlFirmado = facturacionService.generarXmlFirmado(comprobante);
            return ResponseEntity.ok(xmlFirmado);
        } catch (Exception e) {
            log.error("Error firmando XML: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Obtiene el siguiente correlativo disponible
     * GET /api/sunat/correlativo?emisorId=1&tipoComprobante=01&serie=F001
     */
    @GetMapping("/correlativo")
    public ResponseEntity<Map<String, Object>> obtenerSiguienteCorrelativo(
            @RequestParam Long emisorId,
            @RequestParam String tipoComprobante,
            @RequestParam String serie) {
        Integer correlativo = facturacionService.obtenerSiguienteCorrelativo(emisorId, tipoComprobante, serie);
        return ResponseEntity.ok(Map.of(
                "emisorId", emisorId,
                "tipoComprobante", tipoComprobante,
                "serie", serie,
                "correlativo", correlativo
        ));
    }

    /**
     * Lista comprobantes de un emisor
     * GET /api/sunat/comprobantes?emisorId=1
     */
    @GetMapping("/comprobantes")
    public ResponseEntity<List<Comprobante>> listarComprobantes(@RequestParam Long emisorId) {
        List<Comprobante> comprobantes = facturacionService.listarComprobantes(emisorId);
        return ResponseEntity.ok(comprobantes);
    }

    /**
     * Consulta el estado de un ticket
     * GET /api/sunat/ticket/{ticket}?emisorId=1
     */
    @GetMapping("/ticket/{ticket}")
    public ResponseEntity<ConsultaTicketResponse> consultarTicket(
            @PathVariable String ticket,
            @RequestParam Long emisorId) {
        log.info("Consultando ticket: {} para emisor ID: {}", ticket, emisorId);
        Emisor emisor = emisorService.obtenerPorId(emisorId);
        ConsultaTicketResponse response = sunatService.consultarTicket(ticket, emisor);
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
