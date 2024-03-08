package io.github.mengfly.excel.report.component.chart.data;

import lombok.Data;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;

@Data
public class ChartMarker {
    private boolean showPercent = true;
    private boolean showBubbleSize = false;
    private boolean showLeaderLines = false;
    private boolean showSerName = false;
    private boolean showCatName = false;
    private boolean showVal = true;
    private boolean showLegendKey = false;

    public void initMarker(CTDLbls ctdLbls) {
        ctdLbls.addNewShowBubbleSize().setVal(showBubbleSize);
        ctdLbls.addNewShowLeaderLines().setVal(showLeaderLines);
        ctdLbls.addNewShowSerName().setVal(showSerName);
        ctdLbls.addNewShowCatName().setVal(showCatName);
        ctdLbls.addNewShowVal().setVal(showVal);
        ctdLbls.addNewShowLegendKey().setVal(showLegendKey);
        ctdLbls.addNewShowPercent().setVal(showPercent);
    }
}
