package io.github.mengfly.excel.report.component.chart.data;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;

@Data
@Accessors(chain = true)
public abstract class ChartAxisData<T extends XDDFDataSource<?>> {


    private String name;
    private final AxisDataResolver resolver;

    public int dataCount() {
        return resolver.dataCount();
    }

    @SuppressWarnings("unchecked")
    public T createDataSource(ChartDataContext context) {
        return ((T) resolver.createDataSource(context));
    }

    public void initSeriesStyle(XDDFChartData.Series series) {
        // TODO  2023/12/29
        series.setTitle(name, null);
    }
}
