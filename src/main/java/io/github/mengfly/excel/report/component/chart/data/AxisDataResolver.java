package io.github.mengfly.excel.report.component.chart.data;

import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;

public interface AxisDataResolver {

    int dataCount();

    XDDFDataSource<?> createDataSource(ChartDataContext context);
}
