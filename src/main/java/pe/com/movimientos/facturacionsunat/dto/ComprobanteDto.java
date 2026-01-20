package pe.com.movimientos.facturacionsunat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteDto {

    /**
     * Tipo de comprobante
     * 01 = Factura
     * 03 = Boleta
     * 07 = Nota de Credito
     * 08 = Nota de Debito
     */
    @Builder.Default
    private String tipoComprobante = "01";

    /**
     * Serie del comprobante (F001, B001, etc)
     */
    private String serie;

    /**
     * Numero correlativo
     */
    private Integer correlativo;

    /**
     * Fecha de emision
     */
    @Builder.Default
    private LocalDate fechaEmision = LocalDate.now();

    /**
     * Tipo de moneda (PEN = Soles, USD = Dolares)
     */
    @Builder.Default
    private String moneda = "PEN";

    /**
     * Datos del cliente
     */
    private ClienteDto cliente;

    /**
     * Items del comprobante
     */
    private List<ItemFacturaDto> items;

    /**
     * Observaciones (opcional)
     */
    private String observaciones;

    // Campos calculados automaticamente
    private BigDecimal totalGravadas;
    private BigDecimal totalIgv;
    private BigDecimal totalVenta;
}
