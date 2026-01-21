package pe.com.movimientos.facturacionsunat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.movimientos.facturacionsunat.dto.EmisorDto;
import pe.com.movimientos.facturacionsunat.entity.Emisor;
import pe.com.movimientos.facturacionsunat.repository.EmisorRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmisorService {

    private final EmisorRepository emisorRepository;

    public Emisor obtenerPorId(Long id) {
        return emisorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Emisor no encontrado con ID: " + id));
    }

    public Emisor obtenerPorRuc(String ruc) {
        return emisorRepository.findByRucAndActivoTrue(ruc)
                .orElseThrow(() -> new RuntimeException("Emisor no encontrado con RUC: " + ruc));
    }

    public List<Emisor> listarTodos() {
        return emisorRepository.findAll();
    }

    @Transactional
    public Emisor crear(EmisorDto dto) {
        if (emisorRepository.existsByRuc(dto.getRuc())) {
            throw new RuntimeException("Ya existe un emisor con el RUC: " + dto.getRuc());
        }

        Emisor emisor = Emisor.builder()
                .ruc(dto.getRuc())
                .razonSocial(dto.getRazonSocial())
                .nombreComercial(dto.getNombreComercial())
                .direccion(dto.getDireccion())
                .ubigeo(dto.getUbigeo())
                .departamento(dto.getDepartamento())
                .provincia(dto.getProvincia())
                .distrito(dto.getDistrito())
                .usuarioSol(dto.getUsuarioSol())
                .claveSol(dto.getClaveSol())
                .claveCertificado(dto.getClaveCertificado())
                .ambiente(dto.getAmbiente() != null ? dto.getAmbiente() : "BETA")
                .seriesFactura(dto.getSeriesFactura())
                .seriesBoleta(dto.getSeriesBoleta())
                .activo(true)
                .build();

        return emisorRepository.save(emisor);
    }

    @Transactional
    public Emisor actualizar(Long id, EmisorDto dto) {
        Emisor emisor = obtenerPorId(id);
        if (dto.getRuc() != null) emisor.setRuc(dto.getRuc());
        if (dto.getRazonSocial() != null) emisor.setRazonSocial(dto.getRazonSocial());
        if (dto.getNombreComercial() != null) emisor.setNombreComercial(dto.getNombreComercial());
        if (dto.getDireccion() != null) emisor.setDireccion(dto.getDireccion());
        if (dto.getUbigeo() != null) emisor.setUbigeo(dto.getUbigeo());
        if (dto.getDepartamento() != null) emisor.setDepartamento(dto.getDepartamento());
        if (dto.getProvincia() != null) emisor.setProvincia(dto.getProvincia());
        if (dto.getDistrito() != null) emisor.setDistrito(dto.getDistrito());
        if (dto.getUsuarioSol() != null) emisor.setUsuarioSol(dto.getUsuarioSol());
        if (dto.getClaveSol() != null) emisor.setClaveSol(dto.getClaveSol());
        if (dto.getClaveCertificado() != null) emisor.setClaveCertificado(dto.getClaveCertificado());
        if (dto.getAmbiente() != null) emisor.setAmbiente(dto.getAmbiente());
        if (dto.getSeriesFactura() != null) emisor.setSeriesFactura(dto.getSeriesFactura());
        if (dto.getSeriesBoleta() != null) emisor.setSeriesBoleta(dto.getSeriesBoleta());

        return emisorRepository.save(emisor);
    }

    @Transactional
    public Emisor subirCertificado(Long id, MultipartFile archivo, String clave) {
        try {
            Emisor emisor = obtenerPorId(id);
            emisor.setCertificadoPfx(archivo.getBytes());
            emisor.setClaveCertificado(clave);
            log.info("Certificado subido para emisor: {}", emisor.getRuc());
            return emisorRepository.save(emisor);
        } catch (Exception e) {
            throw new RuntimeException("Error al subir certificado: " + e.getMessage());
        }
    }

    @Transactional
    public void desactivar(Long id) {
        Emisor emisor = obtenerPorId(id);
        emisor.setActivo(false);
        emisorRepository.save(emisor);
    }
}
