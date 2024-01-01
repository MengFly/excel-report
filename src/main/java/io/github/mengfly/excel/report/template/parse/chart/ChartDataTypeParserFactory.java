package io.github.mengfly.excel.report.template.parse.chart;

import io.github.mengfly.excel.report.component.chart.type.ChartDataType;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ChartDataTypeParserFactory {

    private static final Map<String, ChartDataTypeParser> PARSER_MAP = new HashMap<>();

    static {
        addParser(new DefaultChartDataTypeParser());
        addParser(new Default3dChartDataTypeParser());
        addParser(new ChartPieTypeParser());
    }

    private static void addParser(ChartDataTypeParser parser) {
        PARSER_MAP.put(parser.getTagName(), parser);
    }

    public static ChartDataType doParse(ContainerTreeNode containerTreeNode, DataContext context) {

        final ChartDataTypeParser parser = PARSER_MAP.get(containerTreeNode.getTagName());
        if (parser == null) {
            log.error("Un support chart type: {}", containerTreeNode.getTagName());
            return null;
        }

        final ChartDataType result = parser.parse(containerTreeNode, context);

        if (result != null) {
            containerTreeNode.initProperties(result, context);
        }
        return result;

    }
}
