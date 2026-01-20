package pe.com.movimientos.facturacionsunat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvioComprobanteRequest {

    /**
     * Nombre del archivo XML (ej: 20123456789-01-F001-00000001.xml)
     * Formato: RUC-TIPO_DOC-SERIE-CORRELATIVO.xml
     */
    private String nombreArchivo;

    /**
     * Contenido del XML del comprobante (puede ser texto plano o base64)
     */
    private String contenidoXml;
}
