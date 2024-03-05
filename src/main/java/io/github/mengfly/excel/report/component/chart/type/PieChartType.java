package io.github.mengfly.excel.report.component.chart.type;

import cn.hutool.core.collection.CollectionUtil;
import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.data.ChartDataContext;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Data;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPie3DChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;

import java.util.Set;

@Data
public class PieChartType implements ChartDataType {

    private ChartLabelAxis labelAxis;
    private ChartValueAxis valueAxis;

    private boolean showPercent = true;
    private boolean showBubbleSize = true;
    private boolean showLeaderLines = true;
    private boolean showSerName = false;
    private boolean showCatName = true;
    private boolean showVal = true;
    private boolean showLegendKey = true;

    @Override
    public Set<ChartTypes> supportChartTypes() {
        return CollectionUtil.newHashSet(ChartTypes.PIE, ChartTypes.PIE3D);
    }

    @Override
    public boolean needCreateChart() {
        if (valueAxis == null) {
            return false;
        }
        if (valueAxis.getDataList().isEmpty()) {
            return false;
        }
        final Set<ChartTypes> chartTypes = supportChartTypes();
        for (ChartValueAxisData chartValueAxisData : valueAxis.getDataList()) {
            if (chartTypes.contains(chartValueAxisData.getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onExport(ReportContext context, Point point, XSSFChart chart) {
        labelAxis = initialLabelAxisDataByValue(labelAxis, valueAxis);
        final XDDFDataSource<?> labelDataSource = labelAxis.createDataSource(context.getSheet());

        final Set<ChartTypes> chartTypes = supportChartTypes();
        valueAxis.getDataList().stream()
                .filter(chartValueAxisData -> chartTypes.contains(chartValueAxisData.getType()))
                .findFirst().ifPresent(axisData -> {
                    final XDDFChartData data = axisData.createChartData(chart, null, null);

                    final XDDFNumericalDataSource<?> valueDataSource =
                            axisData.createDataSource(new ChartDataContext(context.getSheet(), valueAxis));

                    final XDDFChartData.Series series = data.addSeries(labelDataSource, valueDataSource);
                    axisData.initSeriesStyle(series);


                    chart.plot(data);
                });
        final CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        initMarker(plotArea);

    }

    private void initMarker(CTPlotArea plotArea) {

        for (CTPieChart ctPieChart : plotArea.getPieChartArray()) {
            if (!ctPieChart.isSetDLbls()) {
                initMarker(ctPieChart.addNewDLbls());
            }

        }
        for (CTPie3DChart ctPie3DChart : plotArea.getPie3DChartArray()) {
            if (!ctPie3DChart.isSetDLbls()) {
                initMarker(ctPie3DChart.addNewDLbls());
            }
        }
    }

    private void initMarker(CTDLbls ctdLbls) {

        ctdLbls.addNewShowBubbleSize().setVal(showBubbleSize);
        ctdLbls.addNewShowLeaderLines().setVal(showLeaderLines);
        ctdLbls.addNewShowSerName().setVal(showSerName);
        ctdLbls.addNewShowCatName().setVal(showCatName);
        ctdLbls.addNewShowVal().setVal(showVal);
        ctdLbls.addNewShowLegendKey().setVal(showLegendKey);
        ctdLbls.addNewShowPercent().setVal(showPercent);

    }
}
