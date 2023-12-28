package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

import java.util.HashMap;
import java.util.Map;

public class ParserFactory {

    private static final Map<String, ContainerParser> PARSERS = new HashMap<>();

    static {
        addParser(new TextParser());
        addParser(new TableParser());
        addParser(new ImageParser());
        addParser(new ListParser());
        addParser(new HLayoutParser());
        addParser(new VLayoutParser());
    }

    public static void addParser(ContainerParser parser) {
        if (PARSERS.containsKey(parser.getTagName())) {
            throw new IllegalArgumentException("parser for " + parser.getTagName() + " already exists");
        }
        PARSERS.put(parser.getTagName(), parser);
    }

    public static ContainerParser getParser(String containerType) {
        if (PARSERS.containsKey(containerType)) {
            return PARSERS.get(containerType);
        } else {
            throw new IllegalArgumentException("not found parser for " + containerType);
        }
    }

    public static Container doParseElement(ContainerTreeNode element, DataContext context) {
        return getParser(element.getTagName()).doParse(element, context);
    }
}
