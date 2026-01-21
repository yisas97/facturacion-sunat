package pe.com.movimientos.facturacionsunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.movimientos.facturacionsunat.entity.Emisor;

import java.util.Optional;

@Repository
public interface EmisorRepository extends JpaRepository<Emisor, Long> {

    Optional<Emisor> findByRuc(String ruc);

    Optional<Emisor> findByRucAndActivoTrue(String ruc);

    boolean existsByRuc(String ruc);
}
