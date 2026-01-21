package pe.com.movimientos.facturacionsunat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.com.movimientos.facturacionsunat.entity.Comprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteResponseDto {

    private Long id;
    private Long emisorId;
    private String emisorRuc;
    private String emisorRazonSocial;
    private String tipoComprobante;
    private String serie;
    private Integer correlativo;
    private String numeroCompleto;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String moneda;
    private String tipoOperacion;
    private String formaPago;
    private String clienteTipoDocumento;
    private String clienteNumeroDocumento;
    private String clienteRazonSocial;
    private String clienteDireccion;
    private BigDecimal totalGravadas;
    private BigDecimal totalExoneradas;
    private BigDecimal totalInafectas;
    private BigDecimal totalIgv;
    private BigDecimal totalVenta;
    private String cdrCodigo;
    private String cdrMensaje;
    private String estado;
    private String observaciones;
    private List<ComprobanteItemResponseDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ComprobanteResponseDto fromEntity(Comprobante comprobante) {
        return ComprobanteResponseDto.builder()
                .id(comprobante.getId())
                .emisorId(comprobante.getEmisor().getId())
                .emisorRuc(comprobante.getEmisor().getRuc())
                .emisorRazonSocial(comprobante.getEmisor().getRazonSocial())
                .tipoComprobante(comprobante.getTipoComprobante())
                .serie(comprobante.getSerie())
                .correlativo(comprobante.getCorrelativo())
                .numeroCompleto(comprobante.getNumeroCompleto())
                .fechaEmision(comprobante.getFechaEmision())
                .fechaVencimiento(comprobante.getFechaVencimiento())
                .moneda(comprobante.getMoneda())
                .tipoOperacion(comprobante.getTipoOperacion())
                .formaPago(comprobante.getFormaPago())
                .clienteTipoDocumento(comprobante.getClienteTipoDocumento())
                .clienteNumeroDocumento(comprobante.getClienteNumeroDocumento())
                .clienteRazonSocial(comprobante.getClienteRazonSocial())
                .clienteDireccion(comprobante.getClienteDireccion())
                .totalGravadas(comprobante.getTotalGravadas())
                .totalExoneradas(comprobante.getTotalExoneradas())
                .totalInafectas(comprobante.getTotalInafectas())
                .totalIgv(comprobante.getTotalIgv())
                .totalVenta(comprobante.getTotalVenta())
                .cdrCodigo(comprobante.getCdrCodigo())
                .cdrMensaje(comprobante.getCdrMensaje())
                .estado(comprobante.getEstado())
                .observaciones(comprobante.getObservaciones())
                .items(comprobante.getItems().stream()
                        .map(ComprobanteItemResponseDto::fromEntity)
                        .collect(Collectors.toList()))
                .createdAt(comprobante.getCreatedAt())
                .updatedAt(comprobante.getUpdatedAt())
                .build();
    }
}
