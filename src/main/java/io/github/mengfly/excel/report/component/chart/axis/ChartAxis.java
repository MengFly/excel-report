package io.github.mengfly.excel.report.component.chart.axis;

import io.github.mengfly.excel.report.component.chart.AxisType;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;

@Getter
@Setter
public abstract class ChartAxis {
    private String name;

    public abstract AxisType getType();

    public void initAxisStyle(XDDFValueAxis axis) {
        // TODO  2023/12/29
        axis.setTitle(name);
    }
}
