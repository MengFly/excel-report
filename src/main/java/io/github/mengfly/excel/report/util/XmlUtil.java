package io.github.mengfly.excel.report.util;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.*;
import java.util.stream.Collectors;

public class XmlUtil extends cn.hutool.core.util.XmlUtil {

    public static Map<String, String> getElementNameValueMap(List<Element> element) {
        if (element == null || element.isEmpty()) {
            return Collections.emptyMap();
        }
        return element.stream().collect(Collectors.toMap(
                Node::getNodeName,
                Node::getTextContent, (o, o2) -> o2));
    }

    public static Map<String, String> getAttributeMap(Element element, String... ignoreAttribute) {
        Map<String, String> attributesMap = new HashMap<>();
        final NamedNodeMap attributes = element.getAttributes();
        if (attributes == null) {
            return attributesMap;
        }
        Set<String> ignoreAttributeSet = new HashSet<>();
        if (ignoreAttribute != null) {
            ignoreAttributeSet.addAll(java.util.Arrays.asList(ignoreAttribute));
        }
        for (int i = 0; i < attributes.getLength(); i++) {
            final Node item = attributes.item(i);
            final String nodeName = item.getNodeName();
            if (ignoreAttributeSet.contains(nodeName)) {
                continue;
            }

            final String nodeValue = item.getNodeValue();

            attributesMap.put(nodeName, nodeValue);
        }
        return attributesMap;

    }
}
