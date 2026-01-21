package pe.com.movimientos.facturacionsunat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmisorDto {

    private Long id;

    @NotBlank(message = "El RUC es obligatorio")
    @Size(min = 11, max = 11, message = "El RUC debe tener 11 digitos")
    private String ruc;

    @NotBlank(message = "La razon social es obligatoria")
    private String razonSocial;

    private String nombreComercial;

    private String direccion;

    private String ubigeo;

    private String departamento;

    private String provincia;

    private String distrito;

    @NotBlank(message = "El usuario SOL es obligatorio")
    private String usuarioSol;

    @NotBlank(message = "La clave SOL es obligatoria")
    private String claveSol;

    private String claveCertificado;

    /**
     * BETA o PRODUCCION
     */
    @Builder.Default
    private String ambiente = "BETA";

    /**
     * Series de factura separadas por coma (F001,F002)
     */
    private String seriesFactura;

    /**
     * Series de boleta separadas por coma (B001,B002)
     */
    private String seriesBoleta;

    private Boolean activo;

    // Indica si tiene certificado cargado
    private Boolean tieneCertificado;
}
