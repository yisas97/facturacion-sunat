package pe.com.movimientos.facturacionsunat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaTicketResponse {

    /**
     * Indica si la consulta fue exitosa
     */
    private boolean exitoso;

    /**
     * Código de estado de SUNAT
     * 0 = Procesado correctamente
     * 98 = En proceso
     * 99 = Procesado con errores
     */
    private String codigoEstado;

    /**
     * CDR en base64 (si está disponible)
     */
    private String cdrBase64;

    /**
     * CDR en formato XML (extraído del ZIP)
     */
    private String cdrXml;

    /**
     * Mensaje descriptivo del estado
     */
    private String mensaje;
}
