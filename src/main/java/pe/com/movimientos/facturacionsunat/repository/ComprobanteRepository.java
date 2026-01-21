package pe.com.movimientos.facturacionsunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.movimientos.facturacionsunat.entity.Comprobante;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    // Buscar por emisor, tipo, serie y correlativo
    Optional<Comprobante> findByEmisorIdAndTipoComprobanteAndSerieAndCorrelativo(
            Long emisorId, String tipoComprobante, String serie, Integer correlativo);

    // Obtener siguiente correlativo
    @Query("SELECT COALESCE(MAX(c.correlativo), 0) + 1 FROM Comprobante c " +
            "WHERE c.emisor.id = :emisorId AND c.tipoComprobante = :tipo AND c.serie = :serie")
    Integer obtenerSiguienteCorrelativo(
            @Param("emisorId") Long emisorId,
            @Param("tipo") String tipoComprobante,
            @Param("serie") String serie);

    // Listar por emisor
    List<Comprobante> findByEmisorIdOrderByCreatedAtDesc(Long emisorId);

    // Listar por emisor y fecha
    List<Comprobante> findByEmisorIdAndFechaEmisionBetweenOrderByCreatedAtDesc(
            Long emisorId, LocalDate fechaInicio, LocalDate fechaFin);

    // Listar por emisor y estado
    List<Comprobante> findByEmisorIdAndEstadoOrderByCreatedAtDesc(Long emisorId, String estado);

    // Buscar por numero completo
    @Query("SELECT c FROM Comprobante c WHERE c.emisor.id = :emisorId " +
            "AND CONCAT(c.serie, '-', c.correlativo) LIKE %:numero%")
    List<Comprobante> buscarPorNumero(@Param("emisorId") Long emisorId, @Param("numero") String numero);
}
