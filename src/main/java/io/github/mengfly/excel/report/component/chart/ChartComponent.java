package io.github.mengfly.excel.report.component.chart;

import cn.hutool.core.collection.CollectionUtil;
import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.data.ChartDataContext;
import io.github.mengfly.excel.report.component.chart.data.ChartLabelAxisData;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.component.chart.data.ValueAxisDataResolver;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class ChartComponent extends AbstractComponent {

    private Size size = Size.of(4, 10);
    private String title;
    private boolean titleOverlay = false;

    private ChartLabelAxis labelAxis;
    private List<ChartValueAxis> valueAxisList;


    @Override
    public void onExport(ReportContext context, Point point) {

        if (CollectionUtil.isEmpty(valueAxisList)) {
            return;
        }

        final ExcelCellSpan cellSpan = context.getCellSpan(point, size).merge();
        final XSSFChart chart = createChart(cellSpan, context);

        labelAxis = checkOrCreateLabelAxis(labelAxis);

        final XDDFChartAxis labelAxis = this.labelAxis.getType().createAxis(chart, AxisPosition.BOTTOM);

        final XDDFDataSource<?> labelDatasource =
                this.labelAxis.getData().createDataSource(new ChartDataContext(context.getSheet(), this.labelAxis));

        for (ChartValueAxis chartAxis : valueAxisList) {
            XDDFValueAxis axis = ((XDDFValueAxis) AxisType.VALUE.createAxis(chart, chartAxis.getPosition()));
            axis.setCrossBetween(AxisCrossBetween.BETWEEN);
            axis.crossAxis(labelAxis);
            labelAxis.crossAxis(axis);
            chartAxis.initAxisStyle(axis);
            if (chartAxis.getPosition() == AxisPosition.LEFT) {
                axis.setCrosses(AxisCrosses.AUTO_ZERO);
            } else {
                axis.setCrosses(AxisCrosses.MAX);
            }

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
                    final XDDFNumericalDataSource<?> dataSource = chartLabelAxisData.createDataSource(new ChartDataContext(context.getSheet(), chartAxis));
                    final XDDFChartData.Series series = data.addSeries(labelDatasource, dataSource);
                    chartLabelAxisData.initSeriesStyle(series);
                });

                chart.plot(data);
            });

        }


    }

    private ChartLabelAxis checkOrCreateLabelAxis(ChartLabelAxis labelAxis) {
        if (labelAxis == null) {
            labelAxis = new ChartLabelAxis();
            labelAxis.setName("X");
            labelAxis.setPosition(AxisPosition.BOTTOM);
            labelAxis.setType(AxisType.VALUE);
        }
        if (labelAxis.getData() != null) {
            return labelAxis;
        }

        int valueCount = 0;
        for (ChartValueAxis chartAxis : valueAxisList) {
            for (ChartValueAxisData chartLabelAxisData : chartAxis.getDataList()) {
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

    private XSSFChart createChart(ExcelCellSpan cellSpan, ReportContext context) {
        final XSSFDrawing drawing = context.createDrawingPatriarch();
        XSSFChart chart = drawing.createChart(cellSpan.getFillAnchor());
        chart.setTitleOverlay(titleOverlay);
        chart.setTitleText(title);
        //chart.createData()
        //chart.setTitleFormula();
        //chart.setFloor();
        //chart.setBackWall();
        //chart.setSideWall();
        //chart.setAutoTitleDeleted();
        //chart.setValueRange();
        //chart.setExternalId();
        //chart.setChartIndex();
        return chart;
    }


}
