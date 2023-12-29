package io.github.mengfly.excel.report.component.chart.axis;

import io.github.mengfly.excel.report.component.chart.AxisType;
import io.github.mengfly.excel.report.component.chart.data.ChartLabelAxisData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChartLabelAxis extends ChartAxis {
    private AxisType type;
    private ChartLabelAxisData data;
    
}
