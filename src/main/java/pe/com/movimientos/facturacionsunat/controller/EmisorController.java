package pe.com.movimientos.facturacionsunat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.movimientos.facturacionsunat.dto.EmisorDto;
import pe.com.movimientos.facturacionsunat.entity.Emisor;
import pe.com.movimientos.facturacionsunat.service.EmisorService;

import java.util.List;

@RestController
@RequestMapping("/api/emisores")
@RequiredArgsConstructor
@Slf4j
public class EmisorController {

    private final EmisorService emisorService;

    /**
     * Lista todos los emisores
     * GET /api/emisores
     */
    @GetMapping
    public ResponseEntity<List<Emisor>> listarTodos() {
        return ResponseEntity.ok(emisorService.listarTodos());
    }

    /**
     * Obtiene un emisor por ID
     * GET /api/emisores/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmisorDto> obtenerPorId(@PathVariable Long id) {
        Emisor emisor = emisorService.obtenerPorId(id);
        return ResponseEntity.ok(mapToDto(emisor));
    }

    /**
     * Obtiene un emisor por RUC
     * GET /api/emisores/ruc/{ruc}
     */
    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<EmisorDto> obtenerPorRuc(@PathVariable String ruc) {
        Emisor emisor = emisorService.obtenerPorRuc(ruc);
        return ResponseEntity.ok(mapToDto(emisor));
    }

    /**
     * Crea un nuevo emisor
     * POST /api/emisores
     */
    @PostMapping
    public ResponseEntity<EmisorDto> crear(@Valid @RequestBody EmisorDto dto) {
        log.info("Creando emisor: {}", dto.getRuc());
        Emisor emisor = emisorService.crear(dto);
        return ResponseEntity.ok(mapToDto(emisor));
    }

    /**
     * Actualiza un emisor
     * PUT /api/emisores/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<EmisorDto> actualizar(
            @PathVariable Long id,
            @RequestBody EmisorDto dto) {
        log.info("Actualizando emisor ID: {}", id);
        Emisor emisor = emisorService.actualizar(id, dto);
        return ResponseEntity.ok(mapToDto(emisor));
    }

    /**
     * Sube el certificado digital del emisor
     * POST /api/emisores/{id}/certificado
     */
    @PostMapping("/{id}/certificado")
    public ResponseEntity<EmisorDto> subirCertificado(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("clave") String clave) {
        log.info("Subiendo certificado para emisor ID: {}", id);
        Emisor emisor = emisorService.subirCertificado(id, archivo, clave);
        return ResponseEntity.ok(mapToDto(emisor));
    }

    /**
     * Desactiva un emisor
     * DELETE /api/emisores/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        log.info("Desactivando emisor ID: {}", id);
        emisorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    private EmisorDto mapToDto(Emisor emisor) {
        return EmisorDto.builder()
                .id(emisor.getId())
                .ruc(emisor.getRuc())
                .razonSocial(emisor.getRazonSocial())
                .nombreComercial(emisor.getNombreComercial())
                .direccion(emisor.getDireccion())
                .ubigeo(emisor.getUbigeo())
                .departamento(emisor.getDepartamento())
                .provincia(emisor.getProvincia())
                .distrito(emisor.getDistrito())
                .usuarioSol(emisor.getUsuarioSol())
                .claveSol("*****") // No exponer la clave
                .ambiente(emisor.getAmbiente())
                .seriesFactura(emisor.getSeriesFactura())
                .seriesBoleta(emisor.getSeriesBoleta())
                .activo(emisor.getActivo())
                .tieneCertificado(emisor.getCertificadoPfx() != null && emisor.getCertificadoPfx().length > 0)
                .build();
    }
}
