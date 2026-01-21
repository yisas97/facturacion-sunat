package pe.com.movimientos.facturacionsunat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pe.com.movimientos.facturacionsunat.dto.ComprobanteDto;
import pe.com.movimientos.facturacionsunat.dto.ItemFacturaDto;
import pe.com.movimientos.facturacionsunat.entity.Emisor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class XmlGeneratorService {

    private static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    public String generarXmlFactura(ComprobanteDto comprobante, Emisor emisor) throws Exception {
        // Calcular totales
        calcularTotales(comprobante);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Elemento raiz Invoice
        String invoiceNs = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
        Element invoice = doc.createElementNS(invoiceNs, "Invoice");
        invoice.setPrefix("");
        invoice.setAttribute("xmlns", invoiceNs);
        invoice.setAttribute("xmlns:cac", CAC_NS);
        invoice.setAttribute("xmlns:cbc", CBC_NS);
        invoice.setAttribute("xmlns:ext", EXT_NS);
        invoice.setAttribute("xmlns:ds", DS_NS);
        doc.appendChild(invoice);

        // UBLExtensions (para la firma)
        Element ublExtensions = doc.createElementNS(EXT_NS, "ext:UBLExtensions");
        Element ublExtension = doc.createElementNS(EXT_NS, "ext:UBLExtension");
        Element extensionContent = doc.createElementNS(EXT_NS, "ext:ExtensionContent");
        ublExtension.appendChild(extensionContent);
        ublExtensions.appendChild(ublExtension);
        invoice.appendChild(ublExtensions);

        // Version UBL
        invoice.appendChild(createCbcElement(doc, "UBLVersionID", "2.1"));
        invoice.appendChild(createCbcElement(doc, "CustomizationID", "2.0"));

        // Numero de comprobante
        String numeroComprobante = comprobante.getSerie() + "-" + String.format("%08d", comprobante.getCorrelativo());
        invoice.appendChild(createCbcElement(doc, "ID", numeroComprobante));

        // Fecha y hora
        invoice.appendChild(createCbcElement(doc, "IssueDate", comprobante.getFechaEmision().format(DateTimeFormatter.ISO_DATE)));
        invoice.appendChild(createCbcElement(doc, "IssueTime", "00:00:00"));

        // Tipo de comprobante con atributos completos
        Element invoiceTypeCode = createCbcElement(doc, "InvoiceTypeCode", comprobante.getTipoComprobante());
        invoiceTypeCode.setAttribute("listAgencyName", "PE:SUNAT");
        invoiceTypeCode.setAttribute("listName", "Tipo de Documento");
        invoiceTypeCode.setAttribute("listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01");
        invoiceTypeCode.setAttribute("listID", comprobante.getTipoOperacion());
        invoiceTypeCode.setAttribute("name", "Tipo de Operacion");
        invoice.appendChild(invoiceTypeCode);

        // Observaciones
        if (comprobante.getObservaciones() != null) {
            Element note = createCbcElement(doc, "Note", comprobante.getObservaciones());
            note.setAttribute("languageLocaleID", "1000");
            invoice.appendChild(note);
        }

        // Moneda con atributos
        Element documentCurrencyCode = createCbcElement(doc, "DocumentCurrencyCode", comprobante.getMoneda());
        documentCurrencyCode.setAttribute("listID", "ISO 4217 Alpha");
        documentCurrencyCode.setAttribute("listName", "Currency");
        documentCurrencyCode.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");
        invoice.appendChild(documentCurrencyCode);

        // Cantidad de items
        invoice.appendChild(createCbcElement(doc, "LineCountNumeric", String.valueOf(comprobante.getItems().size())));

        // Firma (referencia)
        invoice.appendChild(createSignature(doc, emisor));

        // Datos del emisor
        invoice.appendChild(createEmisor(doc, emisor));

        // Datos del cliente
        invoice.appendChild(createCliente(doc, comprobante));

        // Condiciones de pago
        Element paymentTerms = createCacElement(doc, "PaymentTerms");
        paymentTerms.appendChild(createCbcElement(doc, "ID", "FormaPago"));
        paymentTerms.appendChild(createCbcElement(doc, "PaymentMeansID", comprobante.getFormaPago()));
        invoice.appendChild(paymentTerms);

        // Totales de impuestos
        invoice.appendChild(createTaxTotal(doc, comprobante));

        // Total del documento
        invoice.appendChild(createLegalMonetaryTotal(doc, comprobante));

        // Items
        int lineNumber = 1;
        for (ItemFacturaDto item : comprobante.getItems()) {
            invoice.appendChild(createInvoiceLine(doc, item, lineNumber++, comprobante.getMoneda()));
        }

        // Convertir a String (sin INDENT para que la firma no se rompa)
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString();
    }

    private void calcularTotales(ComprobanteDto comprobante) {
        BigDecimal totalGravadas = BigDecimal.ZERO;
        BigDecimal totalExoneradas = BigDecimal.ZERO;
        BigDecimal totalInafectas = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;

        for (ItemFacturaDto item : comprobante.getItems()) {
            BigDecimal subtotal = item.getPrecioUnitario().multiply(item.getCantidad()).setScale(2, RoundingMode.HALF_UP);
            item.setSubtotal(subtotal);

            String tipoAfectacion = item.getTipoAfectacionIgv() != null ? item.getTipoAfectacionIgv() : "10";
            BigDecimal porcentajeIgv = item.getPorcentajeIgv() != null ? item.getPorcentajeIgv() : new BigDecimal("18");

            if (tipoAfectacion.startsWith("1")) { // Gravado (10, 11, 12, etc.)
                totalGravadas = totalGravadas.add(subtotal);
                BigDecimal igvItem = subtotal.multiply(porcentajeIgv).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                item.setIgv(igvItem);
                item.setTotal(subtotal.add(igvItem));
                totalIgv = totalIgv.add(igvItem);
            } else if (tipoAfectacion.startsWith("2")) { // Exonerado
                totalExoneradas = totalExoneradas.add(subtotal);
                item.setIgv(BigDecimal.ZERO);
                item.setTotal(subtotal);
            } else if (tipoAfectacion.startsWith("3")) { // Inafecto
                totalInafectas = totalInafectas.add(subtotal);
                item.setIgv(BigDecimal.ZERO);
                item.setTotal(subtotal);
            }
        }

        comprobante.setTotalGravadas(totalGravadas);
        comprobante.setTotalExoneradas(totalExoneradas);
        comprobante.setTotalInafectas(totalInafectas);
        comprobante.setTotalIgv(totalIgv);
        comprobante.setTotalVenta(totalGravadas.add(totalExoneradas).add(totalInafectas).add(totalIgv));
    }

    private Element createCbcElement(Document doc, String name, String value) {
        Element element = doc.createElementNS(CBC_NS, "cbc:" + name);
        element.setTextContent(value);
        return element;
    }

    private Element createCacElement(Document doc, String name) {
        return doc.createElementNS(CAC_NS, "cac:" + name);
    }

    private Element createSignature(Document doc, Emisor emisor) {
        Element signature = createCacElement(doc, "Signature");
        signature.appendChild(createCbcElement(doc, "ID", "IDSignature"));

        Element signatoryParty = createCacElement(doc, "SignatoryParty");
        Element partyIdentification = createCacElement(doc, "PartyIdentification");
        partyIdentification.appendChild(createCbcElement(doc, "ID", emisor.getRuc()));
        signatoryParty.appendChild(partyIdentification);

        Element partyName = createCacElement(doc, "PartyName");
        partyName.appendChild(createCbcElement(doc, "Name", emisor.getRazonSocial()));
        signatoryParty.appendChild(partyName);
        signature.appendChild(signatoryParty);

        Element digitalSignatureAttachment = createCacElement(doc, "DigitalSignatureAttachment");
        Element externalReference = createCacElement(doc, "ExternalReference");
        externalReference.appendChild(createCbcElement(doc, "URI", "#SignatureValue"));
        digitalSignatureAttachment.appendChild(externalReference);
        signature.appendChild(digitalSignatureAttachment);

        return signature;
    }

    private Element createEmisor(Document doc, Emisor emisor) {
        Element accountingSupplierParty = createCacElement(doc, "AccountingSupplierParty");
        Element party = createCacElement(doc, "Party");

        // Identificacion
        Element partyIdentification = createCacElement(doc, "PartyIdentification");
        Element id = createCbcElement(doc, "ID", emisor.getRuc());
        id.setAttribute("schemeID", "6"); // RUC
        partyIdentification.appendChild(id);
        party.appendChild(partyIdentification);

        // Nombre comercial
        Element partyName = createCacElement(doc, "PartyName");
        String nombreComercial = emisor.getNombreComercial() != null ? emisor.getNombreComercial() : emisor.getRazonSocial();
        partyName.appendChild(createCbcElement(doc, "Name", nombreComercial));
        party.appendChild(partyName);

        // Direccion
        Element partyLegalEntity = createCacElement(doc, "PartyLegalEntity");
        partyLegalEntity.appendChild(createCbcElement(doc, "RegistrationName", emisor.getRazonSocial()));

        Element registrationAddress = createCacElement(doc, "RegistrationAddress");
        registrationAddress.appendChild(createCbcElement(doc, "ID", emisor.getUbigeo() != null ? emisor.getUbigeo() : "150101"));
        registrationAddress.appendChild(createCbcElement(doc, "AddressTypeCode", "0000"));
        registrationAddress.appendChild(createCbcElement(doc, "CityName", emisor.getProvincia() != null ? emisor.getProvincia() : "LIMA"));
        registrationAddress.appendChild(createCbcElement(doc, "CountrySubentity", emisor.getDepartamento() != null ? emisor.getDepartamento() : "LIMA"));
        registrationAddress.appendChild(createCbcElement(doc, "District", emisor.getDistrito() != null ? emisor.getDistrito() : "LIMA"));

        Element addressLine = createCacElement(doc, "AddressLine");
        addressLine.appendChild(createCbcElement(doc, "Line", emisor.getDireccion() != null ? emisor.getDireccion() : "-"));
        registrationAddress.appendChild(addressLine);

        Element country = createCacElement(doc, "Country");
        country.appendChild(createCbcElement(doc, "IdentificationCode", "PE"));
        registrationAddress.appendChild(country);

        partyLegalEntity.appendChild(registrationAddress);
        party.appendChild(partyLegalEntity);
        accountingSupplierParty.appendChild(party);

        return accountingSupplierParty;
    }

    private Element createCliente(Document doc, ComprobanteDto comprobante) {
        Element accountingCustomerParty = createCacElement(doc, "AccountingCustomerParty");
        Element party = createCacElement(doc, "Party");

        // Identificacion
        Element partyIdentification = createCacElement(doc, "PartyIdentification");
        Element id = createCbcElement(doc, "ID", comprobante.getCliente().getNumeroDocumento());
        id.setAttribute("schemeID", comprobante.getCliente().getTipoDocumento());
        partyIdentification.appendChild(id);
        party.appendChild(partyIdentification);

        // Razon social
        Element partyLegalEntity = createCacElement(doc, "PartyLegalEntity");
        partyLegalEntity.appendChild(createCbcElement(doc, "RegistrationName", comprobante.getCliente().getRazonSocial()));
        party.appendChild(partyLegalEntity);

        accountingCustomerParty.appendChild(party);
        return accountingCustomerParty;
    }

    private Element createTaxTotal(Document doc, ComprobanteDto comprobante) {
        Element taxTotal = createCacElement(doc, "TaxTotal");

        Element taxAmount = createCbcElement(doc, "TaxAmount", comprobante.getTotalIgv().toString());
        taxAmount.setAttribute("currencyID", comprobante.getMoneda());
        taxTotal.appendChild(taxAmount);

        // IGV
        if (comprobante.getTotalGravadas().compareTo(BigDecimal.ZERO) > 0) {
            taxTotal.appendChild(createTaxSubtotal(doc, comprobante.getTotalGravadas(), comprobante.getTotalIgv(),
                    comprobante.getMoneda(), "1000", "IGV", "VAT"));
        }

        // Exonerado
        if (comprobante.getTotalExoneradas() != null && comprobante.getTotalExoneradas().compareTo(BigDecimal.ZERO) > 0) {
            taxTotal.appendChild(createTaxSubtotal(doc, comprobante.getTotalExoneradas(), BigDecimal.ZERO,
                    comprobante.getMoneda(), "9997", "EXO", "VAT"));
        }

        // Inafecto
        if (comprobante.getTotalInafectas() != null && comprobante.getTotalInafectas().compareTo(BigDecimal.ZERO) > 0) {
            taxTotal.appendChild(createTaxSubtotal(doc, comprobante.getTotalInafectas(), BigDecimal.ZERO,
                    comprobante.getMoneda(), "9998", "INA", "FRE"));
        }

        return taxTotal;
    }

    private Element createTaxSubtotal(Document doc, BigDecimal taxableAmount, BigDecimal taxAmount,
                                       String moneda, String taxId, String taxName, String taxTypeCode) {
        Element taxSubtotal = createCacElement(doc, "TaxSubtotal");

        Element taxableAmountEl = createCbcElement(doc, "TaxableAmount", taxableAmount.toString());
        taxableAmountEl.setAttribute("currencyID", moneda);
        taxSubtotal.appendChild(taxableAmountEl);

        Element taxAmountEl = createCbcElement(doc, "TaxAmount", taxAmount.toString());
        taxAmountEl.setAttribute("currencyID", moneda);
        taxSubtotal.appendChild(taxAmountEl);

        Element taxCategory = createCacElement(doc, "TaxCategory");
        Element taxScheme = createCacElement(doc, "TaxScheme");
        taxScheme.appendChild(createCbcElement(doc, "ID", taxId));
        taxScheme.appendChild(createCbcElement(doc, "Name", taxName));
        taxScheme.appendChild(createCbcElement(doc, "TaxTypeCode", taxTypeCode));
        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);

        return taxSubtotal;
    }

    private Element createLegalMonetaryTotal(Document doc, ComprobanteDto comprobante) {
        Element legalMonetaryTotal = createCacElement(doc, "LegalMonetaryTotal");

        BigDecimal lineExtension = comprobante.getTotalGravadas()
                .add(comprobante.getTotalExoneradas() != null ? comprobante.getTotalExoneradas() : BigDecimal.ZERO)
                .add(comprobante.getTotalInafectas() != null ? comprobante.getTotalInafectas() : BigDecimal.ZERO);

        Element lineExtensionAmount = createCbcElement(doc, "LineExtensionAmount", lineExtension.toString());
        lineExtensionAmount.setAttribute("currencyID", comprobante.getMoneda());
        legalMonetaryTotal.appendChild(lineExtensionAmount);

        Element taxInclusiveAmount = createCbcElement(doc, "TaxInclusiveAmount", comprobante.getTotalVenta().toString());
        taxInclusiveAmount.setAttribute("currencyID", comprobante.getMoneda());
        legalMonetaryTotal.appendChild(taxInclusiveAmount);

        Element payableAmount = createCbcElement(doc, "PayableAmount", comprobante.getTotalVenta().toString());
        payableAmount.setAttribute("currencyID", comprobante.getMoneda());
        legalMonetaryTotal.appendChild(payableAmount);

        return legalMonetaryTotal;
    }

    private Element createInvoiceLine(Document doc, ItemFacturaDto item, int lineNumber, String moneda) {
        Element invoiceLine = createCacElement(doc, "InvoiceLine");

        invoiceLine.appendChild(createCbcElement(doc, "ID", String.valueOf(lineNumber)));

        Element invoicedQuantity = createCbcElement(doc, "InvoicedQuantity", item.getCantidad().toString());
        invoicedQuantity.setAttribute("unitCode", item.getUnidadMedida() != null ? item.getUnidadMedida() : "NIU");
        invoiceLine.appendChild(invoicedQuantity);

        Element lineExtensionAmount = createCbcElement(doc, "LineExtensionAmount", item.getSubtotal().toString());
        lineExtensionAmount.setAttribute("currencyID", moneda);
        invoiceLine.appendChild(lineExtensionAmount);

        // Precio con IGV
        Element pricingReference = createCacElement(doc, "PricingReference");
        Element alternativeConditionPrice = createCacElement(doc, "AlternativeConditionPrice");
        BigDecimal porcentajeIgv = item.getPorcentajeIgv() != null ? item.getPorcentajeIgv() : new BigDecimal("18");
        BigDecimal precioConIgv = item.getPrecioUnitario().multiply(BigDecimal.ONE.add(porcentajeIgv.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
        Element priceAmount = createCbcElement(doc, "PriceAmount", precioConIgv.toString());
        priceAmount.setAttribute("currencyID", moneda);
        alternativeConditionPrice.appendChild(priceAmount);
        alternativeConditionPrice.appendChild(createCbcElement(doc, "PriceTypeCode", "01"));
        pricingReference.appendChild(alternativeConditionPrice);
        invoiceLine.appendChild(pricingReference);

        // Impuestos del item
        Element taxTotal = createCacElement(doc, "TaxTotal");
        Element taxAmount = createCbcElement(doc, "TaxAmount", item.getIgv().toString());
        taxAmount.setAttribute("currencyID", moneda);
        taxTotal.appendChild(taxAmount);

        Element taxSubtotal = createCacElement(doc, "TaxSubtotal");
        Element taxableAmount = createCbcElement(doc, "TaxableAmount", item.getSubtotal().toString());
        taxableAmount.setAttribute("currencyID", moneda);
        taxSubtotal.appendChild(taxableAmount);

        Element taxAmountSub = createCbcElement(doc, "TaxAmount", item.getIgv().toString());
        taxAmountSub.setAttribute("currencyID", moneda);
        taxSubtotal.appendChild(taxAmountSub);

        Element taxCategory = createCacElement(doc, "TaxCategory");
        taxCategory.appendChild(createCbcElement(doc, "Percent", porcentajeIgv.toString()));
        taxCategory.appendChild(createCbcElement(doc, "TaxExemptionReasonCode", item.getTipoAfectacionIgv() != null ? item.getTipoAfectacionIgv() : "10"));

        Element taxScheme = createCacElement(doc, "TaxScheme");
        taxScheme.appendChild(createCbcElement(doc, "ID", "1000"));
        taxScheme.appendChild(createCbcElement(doc, "Name", "IGV"));
        taxScheme.appendChild(createCbcElement(doc, "TaxTypeCode", "VAT"));
        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);

        taxTotal.appendChild(taxSubtotal);
        invoiceLine.appendChild(taxTotal);

        // Descripcion del item
        Element itemElement = createCacElement(doc, "Item");
        itemElement.appendChild(createCbcElement(doc, "Description", item.getDescripcion()));

        if (item.getCodigo() != null && !item.getCodigo().isEmpty()) {
            Element sellersItemIdentification = createCacElement(doc, "SellersItemIdentification");
            sellersItemIdentification.appendChild(createCbcElement(doc, "ID", item.getCodigo()));
            itemElement.appendChild(sellersItemIdentification);
        }

        invoiceLine.appendChild(itemElement);

        // Precio unitario sin IGV
        Element price = createCacElement(doc, "Price");
        Element priceAmountUnit = createCbcElement(doc, "PriceAmount", item.getPrecioUnitario().toString());
        priceAmountUnit.setAttribute("currencyID", moneda);
        price.appendChild(priceAmountUnit);
        invoiceLine.appendChild(price);

        return invoiceLine;
    }

    public String getNombreArchivo(ComprobanteDto comprobante, Emisor emisor) {
        return String.format("%s-%s-%s-%08d.xml",
                emisor.getRuc(),
                comprobante.getTipoComprobante(),
                comprobante.getSerie(),
                comprobante.getCorrelativo());
    }
}
