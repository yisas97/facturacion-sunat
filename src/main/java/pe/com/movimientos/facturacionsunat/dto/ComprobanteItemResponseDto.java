package pe.com.movimientos.facturacionsunat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.movimientos.facturacionsunat.entity.ComprobanteItem;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteItemResponseDto {

    private Long id;
    private Integer numeroItem;
    private String codigo;
    private String descripcion;
    private String unidadMedida;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioConIgv;
    private String tipoAfectacionIgv;
    private BigDecimal porcentajeIgv;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;

    public static ComprobanteItemResponseDto fromEntity(ComprobanteItem item) {
        return ComprobanteItemResponseDto.builder()
                .id(item.getId())
                .numeroItem(item.getNumeroItem())
                .codigo(item.getCodigo())
                .descripcion(item.getDescripcion())
                .unidadMedida(item.getUnidadMedida())
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .precioConIgv(item.getPrecioConIgv())
                .tipoAfectacionIgv(item.getTipoAfectacionIgv())
                .porcentajeIgv(item.getPorcentajeIgv())
                .subtotal(item.getSubtotal())
                .igv(item.getIgv())
                .total(item.getTotal())
                .build();
    }
}
