package org.cassandraunit.dataset.rows;

import org.cassandraunit.dataset.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XML row datasets, via the JDK's own parser.
 * <p>
 * Deliberately not jackson-dataformat-xml: that would pull woodstox and stax2-api for a format
 * this simple, and this project has a history of dependency-clash reports to be careful about. The
 * JDK parser costs nothing.
 * <p>
 * The shape mirrors the YAML and JSON one:
 * <pre>{@code
 * <dataset>
 *   <table name="widget">
 *     <row>
 *       <id>1690e8da-5bf8-49e8-9583-4dff8a570737</id>
 *       <label>1</label>
 *       <tags><value>alpha</value><value>beta</value></tags>
 *     </row>
 *     <row>
 *       <id>1690e8da-5bf8-49e8-9583-4dff8a570738</id>
 *       <label null="true"/>
 *     </row>
 *   </table>
 * </dataset>
 * }</pre>
 * <p>
 * Every value arrives as a string, which is fine - {@link RowValueConverter} converts against the real
 * column type. XML has no null of its own, hence {@code null="true"}; an element that is simply
 * absent means unset.
 *
 * @author Jeremy Sevellec
 */
public class XmlRowParser implements RowDataSetParser {

    @Override
    public List<TableRows> parse(InputStream in, String defaultTableName, String origin) {
        Element root = read(in, origin);
        List<TableRows> tables = new ArrayList<>();
        for (Element table : children(root)) {
            String name = table.hasAttribute("name") ? table.getAttribute("name") : defaultTableName;
            if (name == null || name.isEmpty()) {
                throw new ParseException(origin + ": <" + table.getTagName() + "> has no name "
                        + "attribute, and the dataset was not loaded with a table name.");
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Element row : children(table)) {
                rows.add(toRow(row));
            }
            tables.add(new TableRows(name, rows));
        }
        return tables;
    }

    private static Element read(InputStream in, String origin) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            // A dataset is data. It has no business pulling in a DTD or an external entity, and
            // disallowing them is what keeps a fixture from reading the filesystem (XXE).
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(in);
            document.getDocumentElement().normalize();
            return document.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ParseException(origin + ": " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> toRow(Element row) {
        Map<String, Object> columns = new LinkedHashMap<>();
        for (Element column : children(row)) {
            columns.put(column.getTagName(), toValue(column));
        }
        return columns;
    }

    private static Object toValue(Element column) {
        if ("true".equalsIgnoreCase(column.getAttribute("null"))) {
            return null;
        }
        List<Element> nested = children(column);
        if (nested.isEmpty()) {
            return column.getTextContent();
        }
        if (nested.stream().allMatch(e -> e.hasAttribute("key"))) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Element entry : nested) {
                map.put(entry.getAttribute("key"), toValue(entry));
            }
            return map;
        }
        List<Object> list = new ArrayList<>(nested.size());
        for (Element element : nested) {
            list.add(toValue(element));
        }
        return list;
    }

    private static List<Element> children(Element parent) {
        NodeList nodes = parent.getChildNodes();
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }
}
