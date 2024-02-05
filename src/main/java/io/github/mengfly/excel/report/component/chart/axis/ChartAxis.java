package io.github.mengfly.excel.report.component.chart.axis;

import io.github.mengfly.excel.report.component.chart.AxisType;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;

@Getter
@Setter
public abstract class ChartAxis {
    private String title;
    private String numberFormat;
    /**
     * @see XDDFChartAxis#setLogBase(double)
     */
    private Double logBase;

    private Double majorUnit;
    private Double minorUnit;
    private Double minimum;
    private Double maximum;
    private AxisOrientation orientation;
    private AxisTickMark majorTickMark;
    private AxisTickMark minorTickMark;
    private AxisTickLabelPosition tickLabelPosition;

    public abstract AxisType getType();

    protected void initAxisStyle(XDDFChartAxis axis) {
        axis.setTitle(title);
        if (numberFormat != null) {
            axis.setNumberFormat(numberFormat);
        }
        if (logBase != null) {
            axis.setLogBase(logBase);
        }
        if (minimum != null) {
            axis.setMinimum(minimum);
        }
        if (maximum != null) {
            axis.setMaximum(maximum);
        }
        if (orientation != null) {
            axis.setOrientation(orientation);
        }
        if (majorTickMark != null) {
            axis.setMajorTickMark(majorTickMark);
        }
        if (minorTickMark != null) {
            axis.setMinorTickMark(minorTickMark);
        }
        if (tickLabelPosition != null) {
            axis.setTickLabelPosition(tickLabelPosition);
        }
        if (majorUnit != null) {
            axis.setMajorUnit(majorUnit);
        }
        if (minorUnit != null) {
            axis.setMinorUnit(minorUnit);
        }
        axis.setVisible(true);

    }

    public XDDFChartAxis createAxis(XSSFChart chart, AxisPosition position) {
        XDDFChartAxis axis = getType().createAxis(chart, position);
        initAxisStyle(axis);
        return axis;
    }
}
