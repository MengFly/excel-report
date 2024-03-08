package io.github.mengfly.excel.report.component.chart.type;

import io.github.mengfly.excel.report.component.chart.AxisType;
import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.component.chart.data.ChartMarker;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.excel.ReportContext;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xssf.usermodel.XSSFChart;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface ChartDataType {
    Set<ChartTypes> supportChartTypes();

    boolean needCreateChart();

    void onExport(ReportContext context, Point point, XSSFChart chart);

    void initMarker(XSSFChart chart, ChartMarker marker);

    default ChartLabelAxis initialLabelAxisDataByValue(ChartLabelAxis labelAxis, ChartValueAxis... valueAxes) {

        if (labelAxis == null) {
            labelAxis = new ChartLabelAxis();
            labelAxis.setTitle("X");
            labelAxis.setType(AxisType.VALUE);
        }
        if (labelAxis.getData() != null) {
            return labelAxis;
        }


        int valueCount = 0;

        if (valueAxes != null) {
            for (ChartValueAxis axis : valueAxes) {
                if (axis == null) {
                    continue;
                }

                for (ChartValueAxisData chartLabelAxisData : axis.getDataList()) {
                    valueCount = Math.max(valueCount, chartLabelAxisData.dataCount());
                }
            }
        }
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < valueCount; i++) {
            values.add(i);
        }
        labelAxis.setData(values);

        return labelAxis;
    }
}
