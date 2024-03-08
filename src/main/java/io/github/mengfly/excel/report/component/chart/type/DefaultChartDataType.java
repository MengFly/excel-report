package io.github.mengfly.excel.report.component.chart.type;

import cn.hutool.core.collection.CollectionUtil;
import io.github.mengfly.excel.report.component.chart.AxisType;
import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.data.ChartDataContext;
import io.github.mengfly.excel.report.component.chart.data.ChartMarker;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Data;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认的图表类型，支持左右两个轴 ，左右两个轴的数据可以是同一个，也可以是不同的
 * <p>
 * 支持的数据图表类型： 折线图，柱状图，饼图，散点图，柱状图，填充折线图
 *
 * @see ChartTypes#AREA
 * @see ChartTypes#BAR
 * @see ChartTypes#LINE
 * @see ChartTypes#SCATTER
 */
@Data
public class DefaultChartDataType implements ChartDataType {

    /**
     * X 轴坐标
     */
    private ChartLabelAxis labelAxis;
    /**
     * 数值坐标1（对应坐标轴上的左/上轴）
     */
    private ChartValueAxis valueAxis1;
    /**
     * 数值坐标2（对应坐标轴上的右/下轴）
     */
    private ChartValueAxis valueAxis2;
    private ChartMarker marker = new ChartMarker();


    @Override
    public boolean needCreateChart() {
        return valueAxis1 != null || valueAxis2 != null;
    }

    @Override
    public void onExport(ReportContext context, Point point, XSSFChart chart) {

        labelAxis = initialLabelAxisDataByValue(labelAxis, valueAxis1, valueAxis2);

        final XDDFChartAxis labelAxis = this.labelAxis.createAxis(chart, AxisPosition.BOTTOM);

        final XDDFDataSource<?> labelDatasource = this.labelAxis.createDataSource(context.getSheet());
        initLeftAxisData(context, chart, labelAxis, labelDatasource);
        initRightAxisData(context, chart, labelAxis, labelDatasource);

        final CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        // 初始化网格线
        initialAxisGridLines(plotArea);
    }


    /**
     * 初始化网格线
     *
     * @param plotArea plotArea
     */
    private void initialAxisGridLines(CTPlotArea plotArea) {

        for (CTCatAx ctCatAx : plotArea.getCatAxArray()) {
            if (labelAxis != null && labelAxis.isShowMajorGridLines()) {
                ctCatAx.addNewMajorGridlines();
            }
            if (labelAxis != null && labelAxis.isShowMinorGridLines()) {
                ctCatAx.addNewMinorGridlines();
            }
        }
        ChartValueAxis[] axisArray = new ChartValueAxis[]{valueAxis1, valueAxis2};
        final CTValAx[] valAxArray = plotArea.getValAxArray();
        for (int i = 0; i < axisArray.length; i++) {
            if (axisArray[i] != null && axisArray[i].isShowMinorGridLines()) {
                if (!valAxArray[i].isSetMinorGridlines()) {
                    valAxArray[i].addNewMinorGridlines();
                }
            }
            if (axisArray[i] != null && axisArray[i].isShowMajorGridLines()) {
                if (!valAxArray[i].isSetMajorGridlines()) {
                    valAxArray[i].addNewMajorGridlines();
                }
            }
        }

    }

    /**
     * 初始化Marker
     *
     * @param marker plotArea
     */
    @Override
    public void initMarker(XSSFChart chart, ChartMarker marker) {
        final CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        // ChartTypes.AREA
        for (CTAreaChart ctAreaChart : plotArea.getAreaChartArray()) {
            marker.initMarker(ctAreaChart.addNewDLbls());
        }
        // ChartTypes.BAR
        for (CTBarChart ctBarChart : plotArea.getBarChartArray()) {
            marker.initMarker(ctBarChart.addNewDLbls());
        }
        // ChartTypes.LINE
        for (CTLineChart ctLineChart : plotArea.getLineChartArray()) {
            marker.initMarker(ctLineChart.addNewDLbls());
        }
        // ChartTypes.SCATTER
        for (CTScatterChart ctSeriesChart : plotArea.getScatterChartArray()) {
            marker.initMarker(ctSeriesChart.addNewDLbls());
        }
        // ChartTypes.AREA3D
        for (CTArea3DChart ctArea3DChart : plotArea.getArea3DChartArray()) {
            marker.initMarker(ctArea3DChart.addNewDLbls());
        }
        // ChartTypes.LINE3D
        for (CTLine3DChart ctLine3DChart : plotArea.getLine3DChartArray()) {
            marker.initMarker(ctLine3DChart.addNewDLbls());
        }
        // ChartTypes.BAR3D
        for (CTBar3DChart ctBar3DChart : plotArea.getBar3DChartArray()) {
            marker.initMarker(ctBar3DChart.addNewDLbls());
        }

    }

    public Set<ChartTypes> supportChartTypes() {
        return CollectionUtil.newHashSet(ChartTypes.AREA, ChartTypes.BAR, ChartTypes.LINE, ChartTypes.SCATTER);
    }


    private void initLeftAxisData(ReportContext context, XSSFChart chart, XDDFChartAxis labelAxis, XDDFDataSource<?> labelDatasource) {
        if (valueAxis1 == null) {
            return;
        }
        XDDFValueAxis axis = ((XDDFValueAxis) AxisType.VALUE.createAxis(chart, AxisPosition.LEFT));

        axis.setCrossBetween(AxisCrossBetween.BETWEEN);
        axis.crossAxis(labelAxis);
        if (valueAxis2 != null) {
            axis.setCrosses(AxisCrosses.AUTO_ZERO);
        }
        initAxisData(context, valueAxis1, chart, labelAxis, labelDatasource, axis);
    }

    private void initRightAxisData(ReportContext context, XSSFChart chart, XDDFChartAxis labelAxis, XDDFDataSource<?> labelDatasource) {
        if (valueAxis2 == null) {
            return;
        }
        XDDFValueAxis axis = ((XDDFValueAxis) AxisType.VALUE.createAxis(chart, AxisPosition.RIGHT));
        axis.setCrossBetween(AxisCrossBetween.BETWEEN);
        axis.crossAxis(labelAxis);
        if (valueAxis1 != null) {
            axis.setCrosses(AxisCrosses.MAX);
        }

        initAxisData(context, valueAxis2, chart, labelAxis, labelDatasource, axis);

    }

    private void initAxisData(ReportContext context,
                              ChartValueAxis chartAxis,
                              XSSFChart chart,
                              XDDFChartAxis labelAxis,
                              XDDFDataSource<?> labelDatasource,
                              XDDFValueAxis axis) {

        chartAxis.initAxisStyle(axis);
        final Set<ChartTypes> chartTypes = supportChartTypes();

        final ChartDataContext dataSourceContext = new ChartDataContext(context.getSheet(), chartAxis);

        // 根据绘图类型分组
        final Map<ChartTypes, ? extends List<? extends ChartValueAxisData>> typeMap =
                chartAxis.getDataList().stream()
                        .filter(data -> chartTypes.contains(data.getType()))
                        .collect(Collectors.groupingBy(ChartValueAxisData::getType));

        typeMap.forEach((chartType, chartAxisData) -> {
            XDDFChartData data = chart.createData(chartType, labelAxis, axis);
            chartAxisData.forEach(valueAxisData -> {
                valueAxisData.initChartDataStyle(data);
                final XDDFNumericalDataSource<?> dataSource = valueAxisData.createDataSource(dataSourceContext);
                final XDDFChartData.Series series = data.addSeries(labelDatasource, dataSource);
                valueAxisData.initSeriesStyle(series);
            });

            chart.plot(data);
        });
    }


}
