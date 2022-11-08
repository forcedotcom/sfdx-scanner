package com.salesforce.graph.ops;

import static com.salesforce.graph.build.CaseSafePropertyUtil.CASE_SAFE_SUFFIX;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.BaseSFVertex;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Utilities that convert {@link BaseSFVertex} objects to json or xml. */
public final class SerializerUtil {
    /**
     * Label is a property on {@link Vertex} and not explicitly added by the BaseSFVertex#properties
     * map. Therefore they don't exist as "Schema" keys. This string is used to add a label
     * attribute to the JSON output.
     */
    public static final String LABEL = "Label";

    /**
     * An arbitrary set of keys that seem non-essential when trying to obtain the rough shape of the
     * AST
     */
    public static final Set<String> NON_ESSENTIAL_KEYS =
            ImmutableSet.of(
                    Schema.BEGIN_COLUMN,
                    Schema.CHILD_INDEX,
                    Schema.DEFINING_TYPE,
                    Schema.DEFINING_TYPE + CASE_SAFE_SUFFIX,
                    Schema.END_LINE,
                    Schema.FILE_NAME,
                    Schema.FIRST_CHILD,
                    Schema.FULL_METHOD_NAME,
                    Schema.FULL_METHOD_NAME + CASE_SAFE_SUFFIX,
                    Schema.LAST_CHILD,
                    Schema.METHOD_NAME + CASE_SAFE_SUFFIX,
                    Schema.MODIFIERS,
                    Schema.NAME + CASE_SAFE_SUFFIX,
                    Schema.STATIC);

    public static final class Json {
        private static final String CHILDREN = "Children";

        /**
         * @return the vertex as a pretty printed json string
         */
        public static String serialize(BaseSFVertex vertex) {
            return serialize(vertex, Collections.emptySet());
        }

        public static String serialize(BaseSFVertex vertex, Set<String> excludedKeys) {
            TreeMap<String, Object> vertexMap = new TreeMap<>();
            buildJson(vertexMap, vertex, excludedKeys);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(vertexMap);
        }

        private static void buildJson(
                TreeMap<String, Object> vertexMap, BaseSFVertex vertex, Set<String> excludedKeys) {
            vertexMap.put(LABEL, vertex.getLabel());
            for (Map.Entry<String, Object> entry : vertex.getProperties().entrySet()) {
                // Skip properties requested by the caller
                if (excludedKeys.contains(entry.getKey())) {
                    continue;
                }
                vertexMap.put(entry.getKey(), entry.getValue());
            }
            if (!vertex.getChildren().isEmpty()) {
                List<TreeMap<String, Object>> children = new ArrayList<>();
                vertexMap.put(CHILDREN, children);
                for (BaseSFVertex child : vertex.getChildren()) {
                    TreeMap<String, Object> childVertexMap = new TreeMap<>();
                    buildJson(childVertexMap, child, excludedKeys);
                    children.add(childVertexMap);
                }
            }
        }
    }

    public static final class Xml {
        private static final String YES = "yes";
        private static final int INDENT = 4;

        /**
         * @return the vertex as a pretty printed xml string
         */
        public static String serialize(BaseSFVertex vertex) {
            return serialize(vertex, Collections.emptySet());
        }

        /**
         * @return the vertex as a pretty printed xml string, the keys are filtered by {@link
         *     #NON_ESSENTIAL_KEYS}
         */
        public static String serializeMinimal(BaseSFVertex vertex) {
            return serialize(vertex, NON_ESSENTIAL_KEYS);
        }

        public static String serialize(BaseSFVertex vertex, Set<String> excludedKeys) {
            Document document = getXmlDocument();
            Element element = document.createElement(vertex.getLabel());
            document.appendChild(element);
            buildXml(document, vertex, element, excludedKeys);
            return transformXmlDocument(document);
        }

        private static void buildXml(
                Document document, BaseSFVertex vertex, Element element, Set<String> excludedKeys) {
            for (Map.Entry<String, Object> entry : vertex.getProperties().entrySet()) {
                // Skip properties requested by the caller
                if (excludedKeys.contains(entry.getKey())) {
                    continue;
                }
                Attr attr = document.createAttribute(entry.getKey());
                attr.setValue(String.valueOf(entry.getValue()));
                element.setAttributeNode(attr);
            }
            for (BaseSFVertex child : vertex.getChildren()) {
                Element childElement = document.createElement(child.getLabel());
                element.appendChild(childElement);
                buildXml(document, child, childElement, excludedKeys);
            }
        }

        private static String transformXmlDocument(Document document) {
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, YES);
                transformer.setOutputProperty(OutputKeys.INDENT, YES);
                transformer.setOutputProperty(
                        "{http://xml.apache.org/xslt}indent-amount", String.valueOf(INDENT));
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(document), new StreamResult(writer));
                return writer.getBuffer().toString();
            } catch (TransformerException ex) {
                throw new UnexpectedException(ex);
            }
        }

        private static Document getXmlDocument() {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.newDocument();
            } catch (ParserConfigurationException ex) {
                throw new UnexpectedException(ex);
            }
        }
    }
}
