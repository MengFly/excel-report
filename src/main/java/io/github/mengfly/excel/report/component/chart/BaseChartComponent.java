package io.github.mengfly.excel.report.component.chart;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.xddf.usermodel.chart.DisplayBlanks;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;

@Getter
@Setter
public abstract class BaseChartComponent extends AbstractComponent {

    private Size size = Size.of(4, 10);
    private String title;
    private Integer floor;
    private Integer backWall;
    private Integer sideWall;
    private Boolean autoTitleDelete;
    private DisplayBlanks displayBlanks;
    private boolean titleOverlay = false;
    private Legend legend;


    protected XSSFChart createChart(ReportContext context, Point point) {

        final ExcelCellSpan cellSpan = context.getCellSpan(point, size).merge();
        final XSSFDrawing drawing = context.createDrawingPatriarch();
        XSSFChart chart = drawing.createChart(cellSpan.getFillAnchor());
        chart.setTitleOverlay(titleOverlay);
        chart.setTitleText(title);

        if (floor != null) {
            chart.setFloor(floor);

        }
        if (backWall != null) {
            chart.setBackWall(backWall);
        }
        if (sideWall != null) {
            chart.setSideWall(sideWall);
        }
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
