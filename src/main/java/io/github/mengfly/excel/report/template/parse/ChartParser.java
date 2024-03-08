package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.chart.ChartComponent;
import io.github.mengfly.excel.report.component.chart.Legend;
import io.github.mengfly.excel.report.component.chart.data.ChartMarker;
import io.github.mengfly.excel.report.component.chart.data.ChartTitle;
import io.github.mengfly.excel.report.component.chart.type.ChartDataType;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.parse.chart.ChartDataTypeParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;

import java.util.Set;

@Slf4j
public class ChartParser extends AbstractLayoutParser {
    private static final Set<String> ignoreTags = Sets.newHashSet("Legend", "Title", "Marker");

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

        // 标题
        component.setChartTitle(containerTreeNode.getChild(context, "Title", ChartTitle::new));
        // 图例
        component.setLegend(containerTreeNode.getChild(context, "Legend", Legend::new));
        // 数据标识
        component.setMarker(containerTreeNode.getChild(context, "Marker", ChartMarker::new));

        component.setSize(getSize(containerTreeNode, context, Size.of(4, 10)));

        return component;
    }

    private ChartDataType getChartDataType(ContainerTreeNode containerTreeNode, DataContext context) {
        ContainerTreeNode dataNode = containerTreeNode.listChild(null)
                .stream().filter(node -> !ignoreTags.contains(node.getTagName())).findFirst().orElse(null);

        if (dataNode == null) {
            return null;
        }

        return ChartDataTypeParserFactory.doParse(dataNode, context);

    }

}
