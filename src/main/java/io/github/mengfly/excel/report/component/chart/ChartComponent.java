package io.github.mengfly.excel.report.component.chart;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.component.chart.data.Marker;
import io.github.mengfly.excel.report.component.chart.type.ChartDataType;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.xddf.usermodel.chart.DisplayBlanks;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;

@Getter
@Setter
@RequiredArgsConstructor
public class ChartComponent extends AbstractComponent {


    @NonNull
    private final ChartDataType type;

    private Size size = Size.of(4, 10);
    private String title;
    private Boolean autoTitleDelete;
    private DisplayBlanks displayBlanks;
    private boolean titleOverlay = false;
    private Legend legend;
    private Marker marker;
    private ClientAnchor.AnchorType anchorType;


    @Override
    public void onExport(ReportContext context, Point point) {

        if (type.needCreateChart()) {
            final XSSFChart chart = createChart(context, point);

            type.onExport(context, point, chart);

            if (marker != null) {
                type.initMarker(chart, marker);
            }

        }

    }

    private XSSFChart createChart(ReportContext context, Point point) {

        final ExcelCellSpan cellSpan = context.getCellSpan(point, size).merge();
        final XSSFDrawing drawing = context.createDrawingPatriarch();
        XSSFChart chart = drawing.createChart(cellSpan.getFillAnchor(anchorType));
        chart.setTitleOverlay(titleOverlay);
        chart.setTitleText(title);

        if (autoTitleDelete != null) {
            chart.setAutoTitleDeleted(autoTitleDelete);
        }
        if (displayBlanks != null) {
            chart.displayBlanksAs(displayBlanks);
        }
        if (legend != null) {
            legend.initLegend(chart.getOrAddLegend());
        }
        return chart;
    }

}
