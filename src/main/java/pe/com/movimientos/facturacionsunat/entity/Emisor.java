package pe.com.movimientos.facturacionsunat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "emisores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Emisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String ruc;

    @Column(nullable = false, length = 200)
    private String razonSocial;

    @Column(length = 200)
    private String nombreComercial;

    @Column(length = 300)
    private String direccion;

    @Column(length = 6)
    private String ubigeo;

    @Column(length = 100)
    private String departamento;

    @Column(length = 100)
    private String provincia;

    @Column(length = 100)
    private String distrito;

    // Credenciales SOL
    @Column(nullable = false, length = 20)
    private String usuarioSol;

    @Column(nullable = false, length = 100)
    private String claveSol;

    // Certificado digital
    @Lob
    @Column(columnDefinition = "VARBINARY(MAX)")
    private byte[] certificadoPfx;

    @Column(length = 100)
    private String claveCertificado;

    // Ambiente: BETA o PRODUCCION
    @Column(length = 20)
    @Builder.Default
    private String ambiente = "BETA";

    // Series autorizadas (JSON o separadas por coma)
    @Column(length = 500)
    private String seriesFactura;  // F001,F002

    @Column(length = 500)
    private String seriesBoleta;   // B001,B002

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
