package io.github.mengfly.excel.report.template.parse.chart;

import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.data.AxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.component.chart.type.ChartDataType;
import io.github.mengfly.excel.report.component.chart.type.DefaultChartDataType;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class DefaultChartDataTypeParser extends ChartDataTypeParser {


    @Override
    String getTagName() {
        return "ChartData";
    }

    protected DefaultChartDataType newDataType() {
        return new DefaultChartDataType();
    }

    @Override
    ChartDataType parse(ContainerTreeNode containerTreeNode, DataContext context) {

        DefaultChartDataType type = newDataType();

        ChartLabelAxis axis = decodeLabelAxis(containerTreeNode, context);

        ChartValueAxis value1Axis = decodeValueAxis(containerTreeNode, context, "valueAxis1");
        ChartValueAxis value2Axis = decodeValueAxis(containerTreeNode, context, "valueAxis2");

        type.setLabelAxis(axis);
        type.setValueAxis1(value1Axis);
        type.setValueAxis2(value2Axis);

        containerTreeNode.initProperties(type, context);
        return type;
    }

    private ChartValueAxis decodeValueAxis(ContainerTreeNode containerTreeNode, DataContext context, String axisTag) {
        final ContainerTreeNode valueAxis = containerTreeNode.getChild(axisTag);
        if (valueAxis != null) {
            AtomicBoolean hasFoundData = new AtomicBoolean(false);
            ChartValueAxis axis = new ChartValueAxis();

            valueAxis.initProperties(axis, context);

            valueAxis.getProcessControl(context, "for").onFetch(childContext -> {

                final List<ContainerTreeNode> dataList = valueAxis.listChild("data");
                for (ContainerTreeNode data : dataList) {
                    final AxisDataResolver resolver = decodeDataNodeResolver(data, childContext);
                    if (resolver.dataCount() > 0) {
                        ChartValueAxisData axisData = axis.addData(resolver);
                        data.initProperties(axisData, childContext, "dataList", "valueType");
                        hasFoundData.set(true);
                    }
                }
            });
            if (hasFoundData.get()) {
                return axis;
            }
        }
        return null;
    }




}
