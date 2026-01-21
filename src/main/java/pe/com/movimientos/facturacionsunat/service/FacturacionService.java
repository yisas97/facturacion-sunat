package pe.com.movimientos.facturacionsunat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.movimientos.facturacionsunat.dto.ComprobanteDto;
import pe.com.movimientos.facturacionsunat.dto.EnvioComprobanteResponse;
import pe.com.movimientos.facturacionsunat.entity.Comprobante;
import pe.com.movimientos.facturacionsunat.entity.ComprobanteItem;
import pe.com.movimientos.facturacionsunat.entity.Emisor;
import pe.com.movimientos.facturacionsunat.repository.ComprobanteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacturacionService {

    private final XmlGeneratorService xmlGeneratorService;
    private final FirmaDigitalService firmaDigitalService;
    private final SunatService sunatService;
    private final EmisorService emisorService;
    private final ComprobanteRepository comprobanteRepository;

    /**
     * Proceso completo: Genera XML, firma y envia a SUNAT
     * Obtiene el emisor de la BD y guarda el comprobante
     */
    @Transactional
    public EnvioComprobanteResponse emitirComprobante(ComprobanteDto comprobante) {
        try {
            // 0. Obtener emisor de la base de datos
            Emisor emisor = emisorService.obtenerPorId(comprobante.getEmisorId());
            log.info("Emisor obtenido: {} - {}", emisor.getRuc(), emisor.getRazonSocial());

            // Verificar que tiene certificado
            if (!firmaDigitalService.tieneCertificado(emisor)) {
                return EnvioComprobanteResponse.builder()
                        .exitoso(false)
                        .mensaje("El emisor no tiene certificado digital configurado")
                        .build();
            }

            log.info("Iniciando emision de comprobante {}-{}", comprobante.getSerie(), comprobante.getCorrelativo());

            // 1. Generar XML con datos del emisor
            log.info("Generando XML...");
            String xmlSinFirma = xmlGeneratorService.generarXmlFactura(comprobante, emisor);
            log.debug("XML generado:\n{}", xmlSinFirma);

            // 2. Firmar XML con certificado del emisor
            log.info("Firmando XML...");
            String xmlFirmado = firmaDigitalService.firmarXml(xmlSinFirma, emisor);
            log.debug("XML firmado:\n{}", xmlFirmado);

            // 3. Obtener nombre de archivo
            String nombreArchivo = generarNombreArchivo(emisor.getRuc(), comprobante);
            log.info("Nombre de archivo: {}", nombreArchivo);

            // 4. Enviar a SUNAT
            log.info("Enviando a SUNAT - Ambiente: {}", emisor.getAmbiente());
            EnvioComprobanteResponse response = sunatService.enviarComprobante(nombreArchivo, xmlFirmado, emisor);

            // 5. Guardar comprobante en BD
            Comprobante comprobanteEntity = guardarComprobante(comprobante, emisor, xmlFirmado, response);
            response.setComprobanteId(comprobanteEntity.getId());

            if (response.isExitoso()) {
                log.info("Comprobante emitido exitosamente: {} - ID: {}", nombreArchivo, comprobanteEntity.getId());
            } else {
                log.error("Error al emitir comprobante: {}", response.getMensaje());
            }

            return response;

        } catch (Exception e) {
            log.error("Error en proceso de emision: {}", e.getMessage(), e);
            return EnvioComprobanteResponse.builder()
                    .exitoso(false)
                    .mensaje("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Solo genera el XML (para pruebas)
     */
    public String generarXmlPreview(ComprobanteDto comprobante) throws Exception {
        Emisor emisor = emisorService.obtenerPorId(comprobante.getEmisorId());
        return xmlGeneratorService.generarXmlFactura(comprobante, emisor);
    }

    /**
     * Genera y firma el XML (para pruebas)
     */
    public String generarXmlFirmado(ComprobanteDto comprobante) throws Exception {
        Emisor emisor = emisorService.obtenerPorId(comprobante.getEmisorId());
        String xmlSinFirma = xmlGeneratorService.generarXmlFactura(comprobante, emisor);
        if (firmaDigitalService.tieneCertificado(emisor)) {
            return firmaDigitalService.firmarXml(xmlSinFirma, emisor);
        }
        return xmlSinFirma;
    }

    /**
     * Obtiene el siguiente correlativo disponible para una serie
     */
    public Integer obtenerSiguienteCorrelativo(Long emisorId, String tipoComprobante, String serie) {
        return comprobanteRepository.obtenerSiguienteCorrelativo(emisorId, tipoComprobante, serie);
    }

    /**
     * Lista comprobantes de un emisor
     */
    public List<Comprobante> listarComprobantes(Long emisorId) {
        return comprobanteRepository.findByEmisorIdOrderByCreatedAtDesc(emisorId);
    }

    private String generarNombreArchivo(String ruc, ComprobanteDto comprobante) {
        return String.format("%s-%s-%s-%08d.xml",
                ruc,
                comprobante.getTipoComprobante(),
                comprobante.getSerie(),
                comprobante.getCorrelativo());
    }

    private Comprobante guardarComprobante(ComprobanteDto dto, Emisor emisor,
            String xmlFirmado, EnvioComprobanteResponse response) {

        Comprobante comprobante = Comprobante.builder()
                .emisor(emisor)
                .tipoComprobante(dto.getTipoComprobante())
                .serie(dto.getSerie())
                .correlativo(dto.getCorrelativo())
                .fechaEmision(dto.getFechaEmision())
                .fechaVencimiento(dto.getFechaVencimiento())
                .moneda(dto.getMoneda())
                .tipoOperacion(dto.getTipoOperacion())
                .formaPago(dto.getFormaPago())
                .clienteTipoDocumento(dto.getCliente().getTipoDocumento())
                .clienteNumeroDocumento(dto.getCliente().getNumeroDocumento())
                .clienteRazonSocial(dto.getCliente().getRazonSocial())
                .clienteDireccion(dto.getCliente().getDireccion())
                .totalGravadas(dto.getTotalGravadas())
                .totalExoneradas(dto.getTotalExoneradas())
                .totalInafectas(dto.getTotalInafectas())
                .totalIgv(dto.getTotalIgv())
                .totalVenta(dto.getTotalVenta())
                .xmlFirmado(xmlFirmado)
                .cdrXml(response.getCdrXml())
                .cdrCodigo(response.getCodigoError())
                .cdrMensaje(response.getMensaje())
                .estado(response.isExitoso() ? "ACEPTADO" : "RECHAZADO")
                .build();

        // Crear items
        List<ComprobanteItem> items = new ArrayList<>();
        for (int i = 0; i < dto.getItems().size(); i++) {
            var itemDto = dto.getItems().get(i);
            ComprobanteItem item = ComprobanteItem.builder()
                    .comprobante(comprobante)
                    .numeroItem(i + 1)
                    .codigo(itemDto.getCodigo())
                    .descripcion(itemDto.getDescripcion())
                    .unidadMedida(itemDto.getUnidadMedida())
                    .cantidad(itemDto.getCantidad())
                    .precioUnitario(itemDto.getPrecioUnitario())
                    .subtotal(itemDto.getSubtotal())
                    .igv(itemDto.getIgv())
                    .tipoAfectacionIgv(itemDto.getTipoAfectacionIgv())
                    .total(itemDto.getTotal())
                    .build();
            items.add(item);
        }
        comprobante.setItems(items);

        return comprobanteRepository.save(comprobante);
    }
}
