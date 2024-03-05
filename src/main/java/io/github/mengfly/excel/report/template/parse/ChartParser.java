package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.chart.ChartComponent;
import io.github.mengfly.excel.report.component.chart.Legend;
import io.github.mengfly.excel.report.component.chart.data.Marker;
import io.github.mengfly.excel.report.component.chart.type.ChartDataType;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.parse.chart.ChartDataTypeParserFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChartParser extends AbstractLayoutParser {
    @Override
    public String getTagName() {
        return "Chart";
    }

    @Override
    protected Container parse(ContainerTreeNode containerTreeNode, DataContext context) {

        ChartDataType type = getChartDataType(containerTreeNode, context);
        if (type == null) {
            log.error("Not found any ChartData.");
            return null;
        }

        ChartComponent component = new ChartComponent(type);
        component.setLegend(getChartLegend(containerTreeNode, context));
        component.setMarker(getChartMarker(containerTreeNode, context));

        component.setSize(getSize(containerTreeNode, context, Size.of(4, 10)));
        return component;
    }

    private ChartDataType getChartDataType(ContainerTreeNode containerTreeNode, DataContext context) {
        ContainerTreeNode dataNode = containerTreeNode.listChild(null)
                .stream().filter(node ->
                        !"Legend".equals(node.getTagName()) &&
                                !"Marker".equals(node.getTagName())
                ).findFirst().orElse(null);

        if (dataNode == null) {
            return null;
        }

        return ChartDataTypeParserFactory.doParse(dataNode, context);

    }

    private Legend getChartLegend(ContainerTreeNode containerTreeNode, DataContext context) {
        final ContainerTreeNode legendNode = containerTreeNode.getChild("Legend");
        if (legendNode == null) {
            return null;
        }

        Legend chartLegend = new Legend();
        legendNode.initProperties(chartLegend, context);

        return chartLegend;
    }

    private Marker getChartMarker(ContainerTreeNode containerTreeNode, DataContext context) {
        final ContainerTreeNode markerNode = containerTreeNode.getChild("Marker");
        if (markerNode == null) {
            return null;
        }

        Marker marker = new Marker();
        markerNode.initProperties(marker, context);

        return marker;
    }
}
