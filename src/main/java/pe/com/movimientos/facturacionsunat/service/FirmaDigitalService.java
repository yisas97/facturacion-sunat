package pe.com.movimientos.facturacionsunat.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class FirmaDigitalService {

    private static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";

    @Value("${sunat.certificado.ruta:classpath:certificado/certificado.pfx}")
    private String certificadoRuta;

    @Value("${sunat.certificado.clave:123456}")
    private String certificadoClave;

    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private X509Certificate certificate;

    public FirmaDigitalService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            cargarCertificado();
        } catch (Exception e) {
            log.warn("No se pudo cargar el certificado: {}. La firma digital no estara disponible.", e.getMessage());
        }
    }

    private void cargarCertificado() throws Exception {
        Resource resource = resourceLoader.getResource(certificadoRuta);
        if (!resource.exists()) {
            throw new RuntimeException("Certificado no encontrado en: " + certificadoRuta);
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, certificadoClave.toCharArray());
        }

        String alias = keyStore.aliases().nextElement();
        privateKey = (PrivateKey) keyStore.getKey(alias, certificadoClave.toCharArray());
        certificate = (X509Certificate) keyStore.getCertificate(alias);

        log.info("Certificado cargado correctamente. Alias: {}", alias);
    }

    public String firmarXml(String xmlSinFirma) throws Exception {
        if (privateKey == null || certificate == null) {
            throw new RuntimeException("Certificado no cargado. Verifique la configuracion del certificado.");
        }

        // Parsear el XML
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xmlSinFirma.getBytes("UTF-8")));
        doc.setXmlStandalone(false);

        // Buscar el nodo ExtensionContent donde se insertara la firma
        NodeList extensionContentList = doc.getElementsByTagNameNS(EXT_NS, "ExtensionContent");
        if (extensionContentList.getLength() == 0) {
            throw new RuntimeException("No se encontro el nodo ExtensionContent en el XML");
        }
        Node extensionContent = extensionContentList.item(0);

        // Crear la firma digital
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Transformaciones: enveloped + canonicalizacion exclusiva
        List<Transform> transforms = new ArrayList<>();
        transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(fac.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null));

        // Referencia al documento completo
        Reference ref = fac.newReference(
                "",
                fac.newDigestMethod(DigestMethod.SHA256, null),
                transforms,
                null,
                null
        );

        // SignedInfo con canonicalizacion exclusiva
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                Collections.singletonList(ref)
        );

        // KeyInfo con el certificado
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        x509Content.add(certificate);
        X509Data xd = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        // Crear el contexto de firma
        DOMSignContext dsc = new DOMSignContext(privateKey, extensionContent);
        dsc.setDefaultNamespacePrefix("ds");

        // Crear y firmar
        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        // Agregar Id al SignatureValue
        NodeList signatureValueList = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "SignatureValue");
        if (signatureValueList.getLength() > 0) {
            Element signatureValue = (Element) signatureValueList.item(0);
            signatureValue.setAttribute("Id", "SignatureValue");
        }

        // Convertir a String SIN modificar el formato (importante!)
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        // NO usar INDENT - eso rompe la firma!

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));

        return baos.toString("UTF-8");
    }

    public boolean isCertificadoDisponible() {
        return privateKey != null && certificate != null;
    }
}
