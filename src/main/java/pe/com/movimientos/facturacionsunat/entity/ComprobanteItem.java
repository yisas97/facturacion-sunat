package pe.com.movimientos.facturacionsunat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "comprobante_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private Comprobante comprobante;

    @Column(nullable = false)
    private Integer numeroItem;

    @Column(length = 50)
    private String codigo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    // Unidad: NIU=Unidad, ZZ=Servicio, KGM=Kilogramo
    @Column(length = 5)
    @Builder.Default
    private String unidadMedida = "NIU";

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidad;

    // Precio unitario SIN IGV
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal precioUnitario;

    // Precio unitario CON IGV (para mostrar)
    @Column(precision = 12, scale = 4)
    private BigDecimal precioConIgv;

    // Tipo afectacion: 10=Gravado, 20=Exonerado, 30=Inafecto
    @Column(length = 2)
    @Builder.Default
    private String tipoAfectacionIgv = "10";

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeIgv = new BigDecimal("18.00");

    // Subtotal (cantidad * precioUnitario)
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    // IGV del item
    @Column(precision = 12, scale = 2)
    private BigDecimal igv;

    // Total del item (subtotal + igv)
    @Column(precision = 12, scale = 2)
    private BigDecimal total;
}
