package pe.com.movimientos.facturacionsunat.dto;

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
     * Codigo del producto
     */
    private String codigo;

    /**
     * Descripcion del producto o servicio
     */
    private String descripcion;

    /**
     * Unidad de medida (NIU = unidad, ZZ = servicio)
     */
    @Builder.Default
    private String unidadMedida = "NIU";

    /**
     * Cantidad
     */
    private BigDecimal cantidad;

    /**
     * Precio unitario SIN IGV
     */
    private BigDecimal precioUnitario;

    /**
     * Tipo de afectacion IGV
     * 10 = Gravado - Operacion Onerosa
     * 20 = Exonerado
     * 30 = Inafecto
     */
    @Builder.Default
    private String tipoAfectacionIgv = "10";
}
