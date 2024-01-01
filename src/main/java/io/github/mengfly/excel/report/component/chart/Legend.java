package io.github.mengfly.excel.report.component.chart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Legend {

    private LegendPosition position;
    private Boolean overlay;


    public void initLegend(XDDFChartLegend legend) {
        if (position != null) {
            legend.setPosition(position);
        }
        if (overlay != null) {
            legend.setOverlay(overlay);
        }
    }
}
