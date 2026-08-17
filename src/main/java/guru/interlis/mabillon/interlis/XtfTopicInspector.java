package guru.interlis.mabillon.interlis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class XtfTopicInspector {

    private XtfTopicInspector() {
    }

    static Inspection inspect(Path xtf) {
        try (InputStream input = Files.newInputStream(xtf)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setNamespaceAware(true);
            Element root = factory.newDocumentBuilder().parse(input).getDocumentElement();
            NodeList baskets = root.getElementsByTagNameNS("http://www.interlis.ch/xtf/2.4/INTERLIS", "datasection");
            Set<String> topics = new HashSet<>();
            if (baskets.getLength() > 0) {
                Node dataSection = baskets.item(0);
                for (Node child = dataSection.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        topics.add(child.getLocalName());
                    }
                }
            }
            return new Inspection(topics, null);
        } catch (IOException | ParserConfigurationException | SAXException | RuntimeException failure) {
            return new Inspection(Set.of(), "XTF-Topicstruktur konnte nicht gelesen werden: "
                    + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()));
        }
    }

    record Inspection(Set<String> topics, String error) {
        boolean contains(ImportScope scope) {
            return topics.contains(scope.topic());
        }
    }
}
