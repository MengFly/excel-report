package io.github.mengfly.excel.report.template.parse.chart;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.data.AxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.RelationAxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.ValueAxisDataResolver;
import io.github.mengfly.excel.report.component.chart.type.ChartDataType;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.util.BeanUtil;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ChartDataTypeParser {

    abstract String getTagName();

    abstract ChartDataType parse(ContainerTreeNode containerTreeNode, DataContext context);


    protected CellRangeAddress decodeAddress(Object address) {
        if (address instanceof CellRangeAddress) {
            return ((CellRangeAddress) address);
        }

        final List<Integer> items = StrUtil.splitTrim(StrUtil.toString(address), ",")
                .stream().map(s -> Convert.toInt(s, 0)).collect(Collectors.toList());

        return new CellRangeAddress(
                ObjectUtil.defaultIfNull(CollectionUtil.get(items, 0), 0),
                ObjectUtil.defaultIfNull(CollectionUtil.get(items, 1), 0),
                ObjectUtil.defaultIfNull(CollectionUtil.get(items, 2), 0),
                ObjectUtil.defaultIfNull(CollectionUtil.get(items, 3), 0)
        );
    }

    protected AxisDataResolver decodeDataNodeResolver(ContainerTreeNode dataNode, DataContext context) {
        AxisDataResolver resolver;
        if ("address".equals(dataNode.getAttribute("valueType"))) {
            resolver = new RelationAxisDataResolver(
                    decodeAddress(context.doExpression(dataNode.getAttribute("address"))));
        } else {
            resolver = new ValueAxisDataResolver(
                    BeanUtil.objectToList(context.doExpression(dataNode.getAttribute("dataList"))));
        }
        return resolver;
    }

    protected ChartLabelAxis decodeLabelAxis(ContainerTreeNode containerTreeNode, DataContext context) {
        final ContainerTreeNode labelAxis = containerTreeNode.getChild("labelAxis");
        if (labelAxis != null) {
            ChartLabelAxis axis = new ChartLabelAxis();

            labelAxis.initProperties(axis, context);

            final ContainerTreeNode data = labelAxis.getChild("data");

            final AxisDataResolver resolver = decodeDataNodeResolver(data, context);
            if (resolver.dataCount() > 0) {
                axis.setData(resolver);
                data.initProperties(axis.getData(), context, "dataList", "type");
                return axis;
            }
        }
        return null;
    }
}
