package guru.interlis.mabillon;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import guru.interlis.mabillon.interlis.ExportXtfRequest;
import guru.interlis.mabillon.interlis.Ili2pgResult;
import guru.interlis.mabillon.interlis.ImportScope;
import guru.interlis.mabillon.interlis.ImportXtfRequest;
import guru.interlis.mabillon.interlis.InterlisToolDefaults;
import guru.interlis.mabillon.interlis.JavaApiIli2pgRunner;
import guru.interlis.mabillon.interlis.JavaApiXtfValidator;
import guru.interlis.mabillon.interlis.SchemaImportRequest;
import guru.interlis.mabillon.interlis.ValidateRequest;
import guru.interlis.mabillon.interlis.ValidationResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Testcontainers
class InterlisSemanticRoundtripIntegrationTest {

    private static final String XTF_INTERLIS_NS = "http://www.interlis.ch/xtf/2.4/INTERLIS";
    private static final String GOLDEN_BUSINESS_BID = "ada09d02-2110-5e46-afa6-ea7426d960bc";
    private static final String GOLDEN_DOSSIER_TID = "dd811d7e-1890-5254-9cd9-0a93bb5635a8";
    private static final String GOLDEN_BUSINESS_TID = "8e2db417-33f6-5818-b052-2b0c91c48f49";
    private static final String GOLDEN_PARTY_TID = "016af2e9-9dc3-5a2d-b032-81fb7353eb0d";

    @Container
    static final PostgreSQLContainer<?> SOURCE = postgres();

    @Container
    static final PostgreSQLContainer<?> TARGET = postgres();

    @TempDir
    Path tempDir;

    @BeforeAll
    static void prepareDatabases() {
        InterlisTestFixture.importGoldenPath(SOURCE);
        require(InterlisTestFixture.runner(TARGET).schemaImport(
                new SchemaImportRequest(InterlisToolDefaults.model(), true, true)), "Ziel-Schemaimport");
    }

    @Test
    void exportImportFreshDatabasePreservesSemanticGraph() {
        JavaApiIli2pgRunner sourceRunner = InterlisTestFixture.runner(SOURCE);
        JavaApiIli2pgRunner targetRunner = InterlisTestFixture.runner(TARGET);
        JavaApiXtfValidator validator = new JavaApiXtfValidator();
        Map<ImportScope, Path> sourceExports = new EnumMap<>(ImportScope.class);

        for (ImportScope scope : ImportScope.values()) {
            Path export = tempDir.resolve("source-" + scope.name().toLowerCase() + ".xtf");
            require(sourceRunner.exportXtf(new ExportXtfRequest(export, scope, List.of())),
                    "Quell-Export " + scope.label());
            require(validator.validate(export), "Quell-XTF-Validierung " + scope.label());
            sourceExports.put(scope, export);
        }

        for (ImportScope scope : ImportScope.values()) {
            Path export = sourceExports.get(scope);
            require(targetRunner.importXtf(new ImportXtfRequest(export, scope, true, true)),
                    "Ziel-Import " + scope.label());
            require(targetRunner.validate(new ValidateRequest(scope)),
                    "Ziel-DB-Validierung " + scope.label());
        }

        for (ImportScope scope : ImportScope.values()) {
            Path reExport = tempDir.resolve("target-" + scope.name().toLowerCase() + ".xtf");
            require(targetRunner.exportXtf(new ExportXtfRequest(reExport, scope, List.of())),
                    "Ziel-Re-Export " + scope.label());
            require(validator.validate(reExport), "Ziel-XTF-Validierung " + scope.label());

            SemanticGraph before = SemanticGraph.read(sourceExports.get(scope));
            SemanticGraph after = SemanticGraph.read(reExport);

            assertThat(before.entries())
                    .as("Semantischer INTERLIS-Graph für %s", scope.label())
                    .isNotEmpty()
                    .isEqualTo(after.entries());

            if (scope == ImportScope.BUSINESS_DATA) {
                assertGoldenPathIdentities(after);
            }
        }
    }

    private static void assertGoldenPathIdentities(SemanticGraph graph) {
        assertThat(graph.entries().keySet())
                .as("Geschäftsdaten-BID bleibt erhalten")
                .anyMatch(key -> key.startsWith("basket:") && key.endsWith("#" + GOLDEN_BUSINESS_BID));
        assertThat(graph.entries().keySet())
                .as("Dossier-TID bleibt erhalten")
                .anyMatch(key -> key.startsWith("object:") && key.endsWith("#" + GOLDEN_DOSSIER_TID));
        assertThat(graph.entries().keySet())
                .as("Geschäft-TID bleibt erhalten")
                .anyMatch(key -> key.startsWith("object:") && key.endsWith("#" + GOLDEN_BUSINESS_TID));
        assertThat(graph.entries().values())
                .as("REF auf den Beteiligten bleibt erhalten")
                .anyMatch(value -> value.contains("}ref=" + GOLDEN_PARTY_TID));
    }

    private static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(
                DockerImageName.parse("sogis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("mabillon")
                .withUsername("mabillon")
                .withPassword("mabillon");
    }

    private static void require(Ili2pgResult result, String operation) {
        if (!result.successful()) {
            throw new AssertionError(operation + " fehlgeschlagen: " + result.diagnostics());
        }
    }

    private static void require(ValidationResult result, String operation) {
        if (!result.valid()) {
            throw new AssertionError(operation + " fehlgeschlagen: " + result.diagnostics());
        }
    }

    private record SemanticGraph(Map<String, String> entries) {

        static SemanticGraph read(Path xtf) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

                Element root = factory.newDocumentBuilder().parse(xtf.toFile()).getDocumentElement();
                Element dataSection = child(root, "datasection");
                Map<String, String> entries = new TreeMap<>();

                for (Element basket : children(dataSection)) {
                    String bid = basket.getAttributeNS(XTF_INTERLIS_NS, "bid");
                    String basketKey = "basket:" + name(basket) + "#" + bid;
                    entries.put(basketKey, canonicalAttributes(basket));

                    for (Element object : children(basket)) {
                        String tid = object.getAttributeNS(XTF_INTERLIS_NS, "tid");
                        String objectKey = "object:" + basketKey + "/" + name(object) + "#" + tid;
                        String previous = entries.put(objectKey, canonical(object));
                        if (previous != null) {
                            throw new IllegalStateException("Doppelter semantischer Schlüssel: " + objectKey);
                        }
                    }
                }
                return new SemanticGraph(Map.copyOf(entries));
            } catch (Exception failure) {
                throw new IllegalStateException("XTF konnte nicht semantisch gelesen werden: " + xtf, failure);
            }
        }

        private static Element child(Element parent, String localName) {
            return children(parent).stream()
                    .filter(element -> localName.equalsIgnoreCase(element.getLocalName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Element fehlt: " + localName));
        }

        private static List<Element> children(Element parent) {
            List<Element> result = new ArrayList<>();
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element element) {
                    result.add(element);
                }
            }
            return result;
        }

        private static String canonical(Element element) {
            StringBuilder result = new StringBuilder();
            result.append('<').append(name(element)).append(canonicalAttributes(element)).append('>');
            NodeList nodes = element.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element child) {
                    result.append(canonical(child));
                } else if (node.getNodeType() == Node.TEXT_NODE) {
                    String text = normalizeText(node.getNodeValue());
                    if (!text.isEmpty()) {
                        result.append(text);
                    }
                }
            }
            return result.append("</").append(name(element)).append('>').toString();
        }

        private static String canonicalAttributes(Element element) {
            NamedNodeMap attributes = element.getAttributes();
            List<String> values = new ArrayList<>();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
                    continue;
                }
                values.add(name(attribute) + "=" + normalizeText(attribute.getNodeValue()));
            }
            values.sort(Comparator.naturalOrder());
            return values.isEmpty() ? "" : "[" + String.join(";", values) + "]";
        }

        private static String name(Node node) {
            String localName = node.getLocalName() == null ? node.getNodeName() : node.getLocalName();
            String namespace = node.getNamespaceURI();
            return "{" + (namespace == null ? "" : namespace) + "}" + localName;
        }

        private static String normalizeText(String value) {
            return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').trim();
        }
    }
}
