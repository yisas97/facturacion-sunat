package pe.com.movimientos.facturacionsunat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvioComprobanteResponse {

    /**
     * Indica si el envío fue exitoso
     */
    private boolean exitoso;

    /**
     * CDR (Constancia de Recepción) en base64
     */
    private String cdrBase64;

    /**
     * CDR en formato XML (extraído del ZIP)
     */
    private String cdrXml;

    /**
     * Ticket para consulta posterior (para resúmenes y bajas)
     */
    private String ticket;

    /**
     * Mensaje descriptivo del resultado
     */
    private String mensaje;

    /**
     * Código de error de SUNAT (si aplica)
     */
    private String codigoError;

    /**
     * ID del comprobante guardado en BD
     */
    private Long comprobanteId;
}
