package io.github.mengfly.excel.report.component.chart;

import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.data.ChartDataContext;
import io.github.mengfly.excel.report.component.chart.data.ChartLabelAxisData;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.component.chart.data.ValueAxisDataResolver;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class ChartComponent extends BaseChartComponent {


    private ChartLabelAxis labelAxis;
    private ChartValueAxis leftAxis;
    private ChartValueAxis rightAxis;

    @Override
    public void onExport(ReportContext context, Point point) {

        if (leftAxis == null && rightAxis == null) {
            return;
        }

        final XSSFChart chart = createChart(context, point);

        labelAxis = checkOrCreateLabelAxis(labelAxis);

        final XDDFChartAxis labelAxis = this.labelAxis.getType().createAxis(chart, AxisPosition.BOTTOM);

        final XDDFDataSource<?> labelDatasource = this.labelAxis.getData()
                .createDataSource(new ChartDataContext(context.getSheet(), this.labelAxis));

        initLeftAxisData(context, chart, labelAxis, labelDatasource);
        initRightAxisData(context, chart, labelAxis, labelDatasource);
    }

    private void initLeftAxisData(ReportContext context, XSSFChart chart, XDDFChartAxis labelAxis, XDDFDataSource<?> labelDatasource) {
        if (leftAxis == null) {
            return;
        }
        XDDFValueAxis axis = ((XDDFValueAxis) AxisType.VALUE.createAxis(chart, AxisPosition.LEFT));
        axis.setCrossBetween(AxisCrossBetween.BETWEEN);
        axis.crossAxis(labelAxis);
        axis.setCrosses(AxisCrosses.AUTO_ZERO);
        initAxisData(context, leftAxis, chart, labelAxis, labelDatasource, axis);
    }

    private void initRightAxisData(ReportContext context, XSSFChart chart, XDDFChartAxis labelAxis, XDDFDataSource<?> labelDatasource) {
        if (rightAxis == null) {
            return;
        }
        XDDFValueAxis axis = ((XDDFValueAxis) AxisType.VALUE.createAxis(chart, AxisPosition.RIGHT));
        axis.setCrossBetween(AxisCrossBetween.BETWEEN);
        axis.crossAxis(labelAxis);
        axis.setCrosses(AxisCrosses.MAX);

        initAxisData(context, rightAxis, chart, labelAxis, labelDatasource, axis);

    }

    private static void initAxisData(ReportContext context,
                                     ChartValueAxis chartAxis,
                                     XSSFChart chart,
                                     XDDFChartAxis labelAxis,
                                     XDDFDataSource<?> labelDatasource,
                                     XDDFValueAxis axis) {

        final ChartDataContext dataSourceContext = new ChartDataContext(context.getSheet(), chartAxis);

        // 根据绘图类型分组
        final Map<ChartTypes, ? extends List<? extends ChartValueAxisData>> typeMap =
                chartAxis.getDataList().stream().collect(Collectors.groupingBy(ChartValueAxisData::getType));

        typeMap.forEach((chartType, chartAxisData) -> {
            XDDFChartData data;

            data = chart.createData(chartType, labelAxis, axis);

            if (data instanceof XDDFBarChartData) {
                ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);
            }

            chartAxisData.forEach(chartLabelAxisData -> {

                final XDDFNumericalDataSource<?> dataSource = chartLabelAxisData.createDataSource(dataSourceContext);
                final XDDFChartData.Series series = data.addSeries(labelDatasource, dataSource);
                chartLabelAxisData.initSeriesStyle(series);
            });

            chart.plot(data);
        });
    }

    private ChartLabelAxis checkOrCreateLabelAxis(ChartLabelAxis labelAxis) {
        if (labelAxis == null) {
            labelAxis = new ChartLabelAxis();
            labelAxis.setName("X");
            labelAxis.setType(AxisType.VALUE);
        }
        if (labelAxis.getData() != null) {
            return labelAxis;
        }

        int valueCount = 0;
        if (leftAxis != null) {
            for (ChartValueAxisData chartLabelAxisData : leftAxis.getDataList()) {
                valueCount = Math.max(valueCount, chartLabelAxisData.dataCount());
            }
        }
        if (rightAxis != null) {
            for (ChartValueAxisData chartLabelAxisData : rightAxis.getDataList()) {
                valueCount = Math.max(valueCount, chartLabelAxisData.dataCount());
            }
        }

        ValueAxisDataResolver data = new ValueAxisDataResolver();
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < valueCount; i++) {
            values.add(i);
        }
        data.setDataList(values);
        labelAxis.setData(new ChartLabelAxisData(data));
        return labelAxis;

    }


}
