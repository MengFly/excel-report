package io.github.mengfly.excel.report.template.parse.chart;

import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.data.AxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.component.chart.type.ChartDataType;
import io.github.mengfly.excel.report.component.chart.type.PieChartType;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

public class ChartPieTypeParser extends ChartDataTypeParser {


    @Override
    String getTagName() {
        return "PieData";
    }

    @Override
    ChartDataType parse(ContainerTreeNode containerTreeNode, DataContext context) {
        PieChartType type = new PieChartType();

        ChartLabelAxis axis = decodeLabelAxis(containerTreeNode, context);
        ChartValueAxis value1Axis = decodeValueAxis(containerTreeNode, context);
        type.setLabelAxis(axis);
        type.setValueAxis(value1Axis);
        
        containerTreeNode.initProperties(type, context);
        return type;
    }

    private ChartValueAxis decodeValueAxis(ContainerTreeNode containerTreeNode, DataContext context) {
        final ContainerTreeNode valueAxis = containerTreeNode.getChild("valueAxis");
        if (valueAxis != null) {

            ChartValueAxis axis = new ChartValueAxis();

            valueAxis.initProperties(axis, context);

            final ContainerTreeNode data = valueAxis.getChild("data");

            final AxisDataResolver resolver = decodeDataNodeResolver(data, context);
            if (resolver.dataCount() > 0) {
                ChartValueAxisData axisData = axis.addData(resolver);
                data.initProperties(axisData, context, "dataList", "valueType");
                return axis;
            }
            return null;
        }
        return null;
    }
}
