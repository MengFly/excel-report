package io.github.mengfly.excel.report.component.chart.data.resolver;

import io.github.mengfly.excel.report.component.chart.data.ChartDataContext;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;

public interface AxisDataResolver {

    int dataCount();

    XDDFDataSource<?> createDataSource(ChartDataContext context);
}
