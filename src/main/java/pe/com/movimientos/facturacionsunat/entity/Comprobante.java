package pe.com.movimientos.facturacionsunat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comprobantes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Emisor emisor;

    // Tipo: 01=Factura, 03=Boleta, 07=NC, 08=ND
    @Column(nullable = false, length = 2)
    private String tipoComprobante;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private Integer correlativo;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    private LocalDate fechaVencimiento;

    @Column(length = 3)
    @Builder.Default
    private String moneda = "PEN";

    // Tipo operacion: 0101=Venta interna, 0200=Exportacion, etc.
    @Column(length = 4)
    @Builder.Default
    private String tipoOperacion = "0101";

    // Forma pago: Contado, Credito
    @Column(length = 20)
    @Builder.Default
    private String formaPago = "Contado";

    // Datos del cliente
    @Column(nullable = false, length = 1)
    private String clienteTipoDocumento;

    @Column(nullable = false, length = 15)
    private String clienteNumeroDocumento;

    @Column(nullable = false, length = 200)
    private String clienteRazonSocial;

    @Column(length = 300)
    private String clienteDireccion;

    // Totales
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalGravadas = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalExoneradas = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalInafectas = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalIgv = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalVenta = BigDecimal.ZERO;

    // XML y CDR
    @Lob
    @Column(columnDefinition = "VARCHAR(MAX)")
    private String xmlFirmado;

    @Lob
    @Column(columnDefinition = "VARCHAR(MAX)")
    private String cdrXml;

    @Column(length = 20)
    private String cdrCodigo;  // 0 = aceptado

    @Column(length = 500)
    private String cdrMensaje;

    // Estado: PENDIENTE, ACEPTADO, RECHAZADO, ANULADO
    @Column(length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(length = 500)
    private String observaciones;

    // Items del comprobante
    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComprobanteItem> items = new ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper para agregar items
    public void addItem(ComprobanteItem item) {
        items.add(item);
        item.setComprobante(this);
    }

    // Genera el nombre del archivo
    public String getNombreArchivo() {
        return String.format("%s-%s-%s-%08d.xml",
                emisor.getRuc(),
                tipoComprobante,
                serie,
                correlativo);
    }

    // Genera el numero completo
    public String getNumeroCompleto() {
        return String.format("%s-%08d", serie, correlativo);
    }
}
