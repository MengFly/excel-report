package io.github.mengfly.excel.report.component.chart;

import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;

public enum AxisType {

    CATEGORY,
    VALUE,
    DATE;


    public XDDFChartAxis createAxis(XSSFChart chart, AxisPosition position) {

        switch (this) {
            case CATEGORY:
                return chart.createCategoryAxis(position);
            case VALUE:
                return chart.createValueAxis(position);
            case DATE:
                return chart.createDateAxis(position);
            default:
                throw new IllegalArgumentException("Unsupported axis type: " + this);
        }
    }
}
