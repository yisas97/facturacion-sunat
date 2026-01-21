package pe.com.movimientos.facturacionsunat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemFacturaDto {

    /**
     * Codigo del producto (opcional)
     */
    private String codigo;

    /**
     * Descripcion del producto o servicio
     */
    @NotBlank(message = "La descripcion es obligatoria")
    private String descripcion;

    /**
     * Unidad de medida (Catalogo 03 SUNAT)
     * NIU = Unidad
     * ZZ = Servicio
     * KGM = Kilogramo
     * LTR = Litro
     * MTR = Metro
     * GLL = Galon
     */
    @Builder.Default
    private String unidadMedida = "NIU";

    /**
     * Cantidad
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    /**
     * Precio unitario SIN IGV
     */
    @NotNull(message = "El precio unitario es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    private BigDecimal precioUnitario;

    /**
     * Tipo de afectacion IGV (Catalogo 07 SUNAT)
     * 10 = Gravado - Operacion Onerosa
     * 11 = Gravado - Retiro por premio
     * 12 = Gravado - Retiro por donacion
     * 20 = Exonerado - Operacion Onerosa
     * 30 = Inafecto - Operacion Onerosa
     * 40 = Exportacion
     */
    @Builder.Default
    private String tipoAfectacionIgv = "10";

    /**
     * Porcentaje de IGV (normalmente 18)
     */
    @Builder.Default
    private BigDecimal porcentajeIgv = new BigDecimal("18");

    // Campos calculados
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;
}
