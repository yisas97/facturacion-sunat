package pe.com.movimientos.facturacionsunat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
     * ID del emisor en base de datos (obligatorio)
     */
    @NotNull(message = "El emisorId es obligatorio")
    private Long emisorId;

    /**
     * Tipo de comprobante
     * 01 = Factura
     * 03 = Boleta
     * 07 = Nota de Credito
     * 08 = Nota de Debito
     */
    @NotBlank(message = "El tipo de comprobante es obligatorio")
    @Builder.Default
    private String tipoComprobante = "01";

    /**
     * Serie del comprobante (F001, B001, etc)
     */
    @NotBlank(message = "La serie es obligatoria")
    private String serie;

    /**
     * Numero correlativo
     */
    @NotNull(message = "El correlativo es obligatorio")
    private Integer correlativo;

    /**
     * Fecha de emision
     */
    @Builder.Default
    private LocalDate fechaEmision = LocalDate.now();

    /**
     * Fecha de vencimiento (para credito)
     */
    private LocalDate fechaVencimiento;

    /**
     * Tipo de moneda (PEN = Soles, USD = Dolares)
     */
    @Builder.Default
    private String moneda = "PEN";

    /**
     * Tipo de operacion (Catalogo 51 SUNAT)
     * 0101 = Venta interna
     * 0200 = Exportacion de bienes
     * 0401 = Ventas no domiciliados
     */
    @Builder.Default
    private String tipoOperacion = "0101";

    /**
     * Forma de pago
     * Contado = Pago al contado
     * Credito = Pago a credito (requiere cuotas)
     */
    @Builder.Default
    private String formaPago = "Contado";

    /**
     * Datos del cliente
     */
    @NotNull(message = "Los datos del cliente son obligatorios")
    @Valid
    private ClienteDto cliente;

    /**
     * Items del comprobante
     */
    @NotEmpty(message = "Debe incluir al menos un item")
    @Valid
    private List<ItemFacturaDto> items;

    /**
     * Observaciones (opcional)
     */
    private String observaciones;

    // Campos calculados automaticamente
    private BigDecimal totalGravadas;
    private BigDecimal totalExoneradas;
    private BigDecimal totalInafectas;
    private BigDecimal totalIgv;
    private BigDecimal totalVenta;
}
