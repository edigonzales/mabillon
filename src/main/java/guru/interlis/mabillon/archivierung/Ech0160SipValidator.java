package guru.interlis.mabillon.archivierung;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

@Component
public final class Ech0160SipValidator implements SipValidator {

    private static final String NS = "http://bar.admin.ch/arelda/v4";

    public Ech0160SipValidator() {
    }

    @Override
    public SipValidationResult validate(Path sipPath) {
        Path packageRoot = sipPath.toAbsolutePath().normalize();
        List<SipValidationMessage> messages = new ArrayList<>();
        Path report = packageRoot.resolveSibling(packageRoot.getFileName() + "-validation-report.txt");
        try {
            Path metadata = packageRoot.resolve("header/metadata.xml");
            if (!Files.isRegularFile(metadata)) {
                messages.add(error("header/metadata.xml fehlt."));
            } else {
                validateSchema(packageRoot, metadata, messages);
                Document document = parse(metadata);
                validateFiles(packageRoot, document, messages);
            }
        } catch (IOException | ParserConfigurationException | SAXException | RuntimeException failure) {
            messages.add(error("SIP-Prüfung konnte nicht durchgeführt werden: " + safeMessage(failure)));
        }

        SipValidationStatus status = messages.stream().anyMatch(message -> message.severity() == SipValidationMessage.Severity.ERROR)
                ? SipValidationStatus.Ungueltig
                : SipValidationStatus.Gueltig;
        writeReport(report, status, messages);
        return new SipValidationResult(status, messages, report);
    }

    private void validateSchema(Path packageRoot, Path metadata, List<SipValidationMessage> messages)
            throws SAXException, IOException {
        Path schemaPath = packageRoot.resolve("header/xsd/arelda.xsd").normalize();
        if (!Files.isRegularFile(schemaPath)) {
            messages.add(error("eCH-0160-Schema fehlt: " + schemaPath));
            return;
        }
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
        var schema = factory.newSchema(new StreamSource(schemaPath.toFile()));
        var validator = schema.newValidator();
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
        validator.setErrorHandler(new CollectingErrorHandler(messages));
        validator.validate(new StreamSource(metadata.toFile()));
    }

    private Document parse(Path metadata) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(metadata.toFile());
    }

    private void validateFiles(Path packageRoot, Document metadata, List<SipValidationMessage> messages)
            throws IOException {
        NodeList files = metadata.getElementsByTagNameNS(NS, "datei");
        for (int index = 0; index < files.getLength(); index++) {
            Element file = (Element) files.item(index);
            if (!(file.getParentNode() instanceof Element folder)) {
                messages.add(error("Datei liegt nicht in einem Ordner."));
                continue;
            }
            String folderName = text(fileParent(folder, "name"));
            String fileName = text(child(file, "name"));
            String checksum = text(child(file, "pruefsumme"));
            if (!safeName(folderName) || !safeName(fileName)) {
                messages.add(error("Unsicherer SIP-Pfad in der Inhaltsübersicht."));
                continue;
            }
            Path target = packageRoot.resolve("content").resolve(folderName).resolve(fileName).normalize();
            if (!target.startsWith(packageRoot.resolve("content").normalize()) || !Files.isRegularFile(target)) {
                messages.add(error("SIP-Datei fehlt: content/" + folderName + "/" + fileName));
                continue;
            }
            if (!checksum.isBlank() && !checksum.equalsIgnoreCase(hash(target))) {
                messages.add(error("SIP-Datei-Hash stimmt nicht: content/" + folderName + "/" + fileName));
            }
        }
    }

    private static Element fileParent(Element folder, String name) {
        return child(folder, name);
    }

    private static Element child(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element && NS.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) {
                return element;
            }
        }
        return null;
    }

    private static String text(Element element) {
        return element == null || element.getTextContent() == null ? "" : element.getTextContent().trim();
    }

    private static boolean safeName(String value) {
        return !value.isBlank() && !value.equals(".") && !value.equals("..")
                && !value.contains("/") && !value.contains("\\") && !value.contains("\u0000");
    }

    private static String hash(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(path));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static SipValidationMessage error(String message) {
        return new SipValidationMessage(SipValidationMessage.Severity.ERROR, message);
    }

    private static String safeMessage(Throwable failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private static void writeReport(Path report, SipValidationStatus status, List<SipValidationMessage> messages) {
        try {
            Files.createDirectories(report.getParent());
            StringBuilder output = new StringBuilder("Status: ").append(status).append('\n');
            for (SipValidationMessage message : messages) {
                output.append(message.severity()).append(": ").append(message.message()).append('\n');
            }
            Files.writeString(report, output.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // The validation result remains authoritative even when the sidecar report cannot be written.
        }
    }

    private static final class CollectingErrorHandler implements ErrorHandler {
        private final List<SipValidationMessage> messages;

        private CollectingErrorHandler(List<SipValidationMessage> messages) {
            this.messages = messages;
        }

        @Override
        public void warning(SAXParseException exception) {
            messages.add(new SipValidationMessage(SipValidationMessage.Severity.WARNING, format(exception)));
        }

        @Override
        public void error(SAXParseException exception) {
            messages.add(Ech0160SipValidator.error(format(exception)));
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            messages.add(Ech0160SipValidator.error(format(exception)));
            throw exception;
        }

        private static String format(SAXParseException exception) {
            return "Zeile " + exception.getLineNumber() + ": " + exception.getMessage();
        }
    }
}
