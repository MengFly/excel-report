package io.github.mengfly.excel.report.component.chart.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;

@Getter
@Setter
@Accessors(chain = true)
public class ChartValueAxisData extends ChartAxisData<XDDFNumericalDataSource<?>> {

    private ChartTypes type;
    private Boolean varyColors;

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
    }
}
