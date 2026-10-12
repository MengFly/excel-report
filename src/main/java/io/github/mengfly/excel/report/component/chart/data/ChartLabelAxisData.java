package io.github.mengfly.excel.report.component.chart.data;

import io.github.mengfly.excel.report.component.chart.data.resolver.AxisDataResolver;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;


public class ChartLabelAxisData extends ChartAxisData<XDDFDataSource<?>> {

    public ChartLabelAxisData(AxisDataResolver resolver) {
        super(resolver);
    }
}
