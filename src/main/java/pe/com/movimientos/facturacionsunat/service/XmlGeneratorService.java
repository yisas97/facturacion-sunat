package pe.com.movimientos.facturacionsunat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pe.com.movimientos.facturacionsunat.dto.ComprobanteDto;
import pe.com.movimientos.facturacionsunat.dto.ItemFacturaDto;

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
@RequiredArgsConstructor
public class XmlGeneratorService {

    private static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    @Value("${sunat.ruc}")
    private String emisorRuc;

    @Value("${sunat.emisor.razonSocial:MI EMPRESA SAC}")
    private String emisorRazonSocial;

    @Value("${sunat.emisor.nombreComercial:MI EMPRESA}")
    private String emisorNombreComercial;

    @Value("${sunat.emisor.ubigeo:150101}")
    private String emisorUbigeo;

    @Value("${sunat.emisor.direccion:AV. PRINCIPAL 123}")
    private String emisorDireccion;

    @Value("${sunat.emisor.departamento:LIMA}")
    private String emisorDepartamento;

    @Value("${sunat.emisor.provincia:LIMA}")
    private String emisorProvincia;

    @Value("${sunat.emisor.distrito:LIMA}")
    private String emisorDistrito;

    public String generarXmlFactura(ComprobanteDto comprobante) throws Exception {
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
        invoiceTypeCode.setAttribute("listID", "0101"); // Catalogo 51 - Venta interna
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
        invoice.appendChild(createSignature(doc));

        // Datos del emisor
        invoice.appendChild(createEmisor(doc));

        // Datos del cliente
        invoice.appendChild(createCliente(doc, comprobante));

        // Condiciones de pago (Contado)
        Element paymentTerms = createCacElement(doc, "PaymentTerms");
        paymentTerms.appendChild(createCbcElement(doc, "ID", "FormaPago"));
        paymentTerms.appendChild(createCbcElement(doc, "PaymentMeansID", "Contado"));
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
        BigDecimal totalIgv = BigDecimal.ZERO;

        for (ItemFacturaDto item : comprobante.getItems()) {
            BigDecimal subtotal = item.getPrecioUnitario().multiply(item.getCantidad());
            if ("10".equals(item.getTipoAfectacionIgv())) {
                totalGravadas = totalGravadas.add(subtotal);
                totalIgv = totalIgv.add(subtotal.multiply(new BigDecimal("0.18")));
            }
        }

        comprobante.setTotalGravadas(totalGravadas.setScale(2, RoundingMode.HALF_UP));
        comprobante.setTotalIgv(totalIgv.setScale(2, RoundingMode.HALF_UP));
        comprobante.setTotalVenta(totalGravadas.add(totalIgv).setScale(2, RoundingMode.HALF_UP));
    }

    private Element createCbcElement(Document doc, String name, String value) {
        Element element = doc.createElementNS(CBC_NS, "cbc:" + name);
        element.setTextContent(value);
        return element;
    }

    private Element createCacElement(Document doc, String name) {
        return doc.createElementNS(CAC_NS, "cac:" + name);
    }

    private Element createSignature(Document doc) {
        Element signature = createCacElement(doc, "Signature");
        signature.appendChild(createCbcElement(doc, "ID", "IDSignature"));

        Element signatoryParty = createCacElement(doc, "SignatoryParty");
        Element partyIdentification = createCacElement(doc, "PartyIdentification");
        partyIdentification.appendChild(createCbcElement(doc, "ID", emisorRuc));
        signatoryParty.appendChild(partyIdentification);

        Element partyName = createCacElement(doc, "PartyName");
        partyName.appendChild(createCbcElement(doc, "Name", emisorRazonSocial));
        signatoryParty.appendChild(partyName);
        signature.appendChild(signatoryParty);

        Element digitalSignatureAttachment = createCacElement(doc, "DigitalSignatureAttachment");
        Element externalReference = createCacElement(doc, "ExternalReference");
        externalReference.appendChild(createCbcElement(doc, "URI", "#SignatureValue"));
        digitalSignatureAttachment.appendChild(externalReference);
        signature.appendChild(digitalSignatureAttachment);

        return signature;
    }

    private Element createEmisor(Document doc) {
        Element accountingSupplierParty = createCacElement(doc, "AccountingSupplierParty");
        Element party = createCacElement(doc, "Party");

        // Identificacion
        Element partyIdentification = createCacElement(doc, "PartyIdentification");
        Element id = createCbcElement(doc, "ID", emisorRuc);
        id.setAttribute("schemeID", "6"); // RUC
        partyIdentification.appendChild(id);
        party.appendChild(partyIdentification);

        // Nombre comercial
        Element partyName = createCacElement(doc, "PartyName");
        partyName.appendChild(createCbcElement(doc, "Name", emisorNombreComercial));
        party.appendChild(partyName);

        // Direccion
        Element partyLegalEntity = createCacElement(doc, "PartyLegalEntity");
        partyLegalEntity.appendChild(createCbcElement(doc, "RegistrationName", emisorRazonSocial));

        Element registrationAddress = createCacElement(doc, "RegistrationAddress");
        registrationAddress.appendChild(createCbcElement(doc, "ID", emisorUbigeo));
        registrationAddress.appendChild(createCbcElement(doc, "AddressTypeCode", "0000"));
        registrationAddress.appendChild(createCbcElement(doc, "CityName", emisorProvincia));
        registrationAddress.appendChild(createCbcElement(doc, "CountrySubentity", emisorDepartamento));
        registrationAddress.appendChild(createCbcElement(doc, "District", emisorDistrito));

        Element addressLine = createCacElement(doc, "AddressLine");
        addressLine.appendChild(createCbcElement(doc, "Line", emisorDireccion));
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
        Element taxSubtotal = createCacElement(doc, "TaxSubtotal");

        Element taxableAmount = createCbcElement(doc, "TaxableAmount", comprobante.getTotalGravadas().toString());
        taxableAmount.setAttribute("currencyID", comprobante.getMoneda());
        taxSubtotal.appendChild(taxableAmount);

        Element taxAmountSub = createCbcElement(doc, "TaxAmount", comprobante.getTotalIgv().toString());
        taxAmountSub.setAttribute("currencyID", comprobante.getMoneda());
        taxSubtotal.appendChild(taxAmountSub);

        Element taxCategory = createCacElement(doc, "TaxCategory");
        Element taxScheme = createCacElement(doc, "TaxScheme");
        taxScheme.appendChild(createCbcElement(doc, "ID", "1000"));
        taxScheme.appendChild(createCbcElement(doc, "Name", "IGV"));
        taxScheme.appendChild(createCbcElement(doc, "TaxTypeCode", "VAT"));
        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);

        taxTotal.appendChild(taxSubtotal);
        return taxTotal;
    }

    private Element createLegalMonetaryTotal(Document doc, ComprobanteDto comprobante) {
        Element legalMonetaryTotal = createCacElement(doc, "LegalMonetaryTotal");

        Element lineExtensionAmount = createCbcElement(doc, "LineExtensionAmount", comprobante.getTotalGravadas().toString());
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
        invoicedQuantity.setAttribute("unitCode", item.getUnidadMedida());
        invoiceLine.appendChild(invoicedQuantity);

        BigDecimal subtotal = item.getPrecioUnitario().multiply(item.getCantidad()).setScale(2, RoundingMode.HALF_UP);
        Element lineExtensionAmount = createCbcElement(doc, "LineExtensionAmount", subtotal.toString());
        lineExtensionAmount.setAttribute("currencyID", moneda);
        invoiceLine.appendChild(lineExtensionAmount);

        // Precio con IGV
        Element pricingReference = createCacElement(doc, "PricingReference");
        Element alternativeConditionPrice = createCacElement(doc, "AlternativeConditionPrice");
        BigDecimal precioConIgv = item.getPrecioUnitario().multiply(new BigDecimal("1.18")).setScale(2, RoundingMode.HALF_UP);
        Element priceAmount = createCbcElement(doc, "PriceAmount", precioConIgv.toString());
        priceAmount.setAttribute("currencyID", moneda);
        alternativeConditionPrice.appendChild(priceAmount);
        alternativeConditionPrice.appendChild(createCbcElement(doc, "PriceTypeCode", "01"));
        pricingReference.appendChild(alternativeConditionPrice);
        invoiceLine.appendChild(pricingReference);

        // Impuestos del item
        Element taxTotal = createCacElement(doc, "TaxTotal");
        BigDecimal igvItem = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        Element taxAmount = createCbcElement(doc, "TaxAmount", igvItem.toString());
        taxAmount.setAttribute("currencyID", moneda);
        taxTotal.appendChild(taxAmount);

        Element taxSubtotal = createCacElement(doc, "TaxSubtotal");
        Element taxableAmount = createCbcElement(doc, "TaxableAmount", subtotal.toString());
        taxableAmount.setAttribute("currencyID", moneda);
        taxSubtotal.appendChild(taxableAmount);

        Element taxAmountSub = createCbcElement(doc, "TaxAmount", igvItem.toString());
        taxAmountSub.setAttribute("currencyID", moneda);
        taxSubtotal.appendChild(taxAmountSub);

        Element taxCategory = createCacElement(doc, "TaxCategory");
        taxCategory.appendChild(createCbcElement(doc, "Percent", "18"));
        taxCategory.appendChild(createCbcElement(doc, "TaxExemptionReasonCode", item.getTipoAfectacionIgv()));

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

        Element sellersItemIdentification = createCacElement(doc, "SellersItemIdentification");
        sellersItemIdentification.appendChild(createCbcElement(doc, "ID", item.getCodigo()));
        itemElement.appendChild(sellersItemIdentification);

        invoiceLine.appendChild(itemElement);

        // Precio unitario sin IGV
        Element price = createCacElement(doc, "Price");
        Element priceAmountUnit = createCbcElement(doc, "PriceAmount", item.getPrecioUnitario().toString());
        priceAmountUnit.setAttribute("currencyID", moneda);
        price.appendChild(priceAmountUnit);
        invoiceLine.appendChild(price);

        return invoiceLine;
    }

    public String getNombreArchivo(ComprobanteDto comprobante) {
        return String.format("%s-%s-%s-%08d.xml",
                emisorRuc,
                comprobante.getTipoComprobante(),
                comprobante.getSerie(),
                comprobante.getCorrelativo());
    }
}
