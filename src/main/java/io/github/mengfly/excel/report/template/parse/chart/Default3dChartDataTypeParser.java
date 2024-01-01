package io.github.mengfly.excel.report.template.parse.chart;

import io.github.mengfly.excel.report.component.chart.type.Default3dChartType;
import io.github.mengfly.excel.report.component.chart.type.DefaultChartDataType;

class Default3dChartDataTypeParser extends DefaultChartDataTypeParser {

    @Override
    String getTagName() {
        return "Chart3dData";
    }

    @Override
    protected DefaultChartDataType newDataType() {
        return new Default3dChartType();
    }
}
