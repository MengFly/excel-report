package io.github.mengfly.excel.report.component.chart.data;

import io.github.mengfly.excel.report.util.XDDFColorHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;

@Getter
@Setter
@Accessors(chain = true)
public class ChartValueAxisData extends ChartAxisData<XDDFNumericalDataSource<?>> {

    private ChartTypes type;
    private Boolean varyColors;

    private boolean smooth = true;
    private Boolean showLeaderLines;
    private String color;

    public ChartValueAxisData(AxisDataResolver resolver) {
        super(resolver);
    }


    public XDDFChartData createChartData(XSSFChart chart, XDDFChartAxis category, XDDFValueAxis values) {
        final XDDFChartData data = chart.createData(getType(), category, values);
        initChartDataStyle(data);
        return data;
    }

    public void initChartDataStyle(XDDFChartData data) {
        if (varyColors != null) {
            data.setVaryColors(varyColors);
        }
        if (data instanceof XDDFBarChartData) {
            ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);
        }
        if (data instanceof XDDFBar3DChartData) {
            ((XDDFBar3DChartData) data).setBarDirection(BarDirection.COL);
        }
    }

    public void initSeriesStyle(XDDFChartData.Series series) {
        if (getTitle() != null) {
            series.setTitle(getTitle(), null);
        }
        if (varyColors == null || !varyColors) {
            series.setFillProperties(new XDDFSolidFillProperties(XDDFColorHelper.createColor(
                    color, PresetColor.GREEN)));
        }
        if (showLeaderLines != null) {
            series.setShowLeaderLines(showLeaderLines);
        }
        if (series instanceof XDDFLineChartData.Series) {
            final XDDFLineChartData.Series lineSeries = (XDDFLineChartData.Series) series;
            lineSeries.setSmooth(smooth);
        }
        if (series instanceof XDDFScatterChartData.Series) {
            final XDDFScatterChartData.Series scatterSeries = (XDDFScatterChartData.Series) series;
            scatterSeries.setSmooth(smooth);
        }
    }
}
