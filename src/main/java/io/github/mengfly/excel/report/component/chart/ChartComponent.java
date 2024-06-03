package io.github.mengfly.excel.report.component.chart;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.component.chart.data.ChartMarker;
import io.github.mengfly.excel.report.component.chart.data.ChartTitle;
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
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartSpace;

@Getter
@Setter
@RequiredArgsConstructor
public class ChartComponent extends AbstractComponent {


    @NonNull
    private final ChartDataType type;

    private Size size = Size.of(4, 10);
    private Boolean autoTitleDelete;
    private DisplayBlanks displayBlanks;
    private Legend legend;
    private ChartMarker marker;
    private ChartTitle chartTitle;
    private ClientAnchor.AnchorType anchorType;


    @Override
    public void onExport(ReportContext context, Point point, Size suggestSize) {

        if (type.needCreateChart()) {
            final XSSFChart chart = createChart(context, point, suggestSize);


            type.onExport(context, point, chart);

            if (marker != null) {
                type.initMarker(chart, marker);
            }

            // 初始化字体
            initChartTitle(chart);

        }

    }

    private void initChartTitle(XSSFChart chart) {
        if (chartTitle != null) {
            chart.setTitleText(chartTitle.getText());
            chart.setTitleOverlay(chartTitle.isOverLay());

            final XDDFTextBody body = chart.getTitle().getBody();

            chartTitle.initTextBodyStyle(body);
        }
    }


    private XSSFChart createChart(ReportContext context, Point point, Size suggestSize) {

        final ExcelCellSpan cellSpan = context.getCellSpan(point, suggestSize).merge();
        final XSSFDrawing drawing = context.createDrawingPatriarch();
        XSSFChart chart = drawing.createChart(cellSpan.getFillAnchor(anchorType));

        final CTChartSpace ctChartSpace = chart.getCTChartSpace();
        ctChartSpace.addNewRoundedCorners().setVal(false);

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
