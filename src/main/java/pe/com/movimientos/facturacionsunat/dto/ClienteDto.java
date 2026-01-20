package pe.com.movimientos.facturacionsunat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDto {

    /**
     * Tipo de documento del cliente
     * 6 = RUC
     * 1 = DNI
     * 4 = Carnet de Extranjeria
     * 7 = Pasaporte
     * 0 = Sin documento (para boletas menores a S/700)
     */
    private String tipoDocumento;

    /**
     * Numero de documento (RUC, DNI, etc)
     */
    private String numeroDocumento;

    /**
     * Razon social o nombre completo
     */
    private String razonSocial;

    /**
     * Direccion del cliente
     */
    private String direccion;

    /**
     * Ubigeo (codigo de distrito)
     */
    private String ubigeo;

    /**
     * Email del cliente (opcional)
     */
    private String email;
}
