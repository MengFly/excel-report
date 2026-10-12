package io.github.mengfly.excel.report.bridge.web;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.bridge.BridgeExporter;
import io.github.mengfly.excel.report.component.chart.ChartComponent;
import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.component.chart.data.resolver.RelationAxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.resolver.ValueAxisDataResolver;
import io.github.mengfly.excel.report.component.chart.type.DefaultChartDataType;
import io.github.mengfly.excel.report.component.image.Base64Image;
import io.github.mengfly.excel.report.component.image.ImageComponent;
import io.github.mengfly.excel.report.component.list.ListComponent;
import io.github.mengfly.excel.report.component.list.ListHeader;
import io.github.mengfly.excel.report.component.text.SpanComponent;
import io.github.mengfly.excel.report.component.text.TextComponent;
import io.github.mengfly.excel.report.entity.Orientation;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.excel.ExportResult;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.layout.VLayout;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.style.key.StyleKey;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

/*
 * WebJsonExporter 的回归用例，钉住"从 Container 反推扁平单元格网格且能真实复现"的几件事：
 *   (a) 几何（行列/跨度）与 position/measuredSize 一致；
 *   (b) 样式值归一化（颜色/枚举/布尔/数字）、未注册 key 原样保留；
 *   (c) 继承样式经 finalStyle 进入 cell.style；
 *   (d) 图片内嵌 base64 dataURL + 渲染参数；
 *   (e) 图表结构化数据（坐标轴/系列值/区域引用）可还原；
 *   (f) 列宽/行高、Sheet 级样式归一化；
 *   (g) 富文本分片、List 展开、空入参安全；
 *   (h) 内容与几何分离：内容统一由 WebCellContent 承载（kind 判别）。
 */
public class WebJsonExporterTest {

    /**
     * 1x1 透明 PNG，用于图片内嵌用例
     */
    private static final String PNG_DATA_URL =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

    private final ExcelReport report = new ExcelReport();

    private ExportResult export(Container container) {
        return report.exportSheet("s", container, new StyleMap());
    }

    private WebCell findCell(WebExcel web, int row, int col) {
        for (WebCell cell : web.getCells()) {
            if (cell.getRow() == row && cell.getCol() == col) {
                return cell;
            }
        }
        return null;
    }

    private WebTextContent text(WebCell cell) {
        Assert.assertTrue("内容应为文本", cell.getContent() instanceof WebTextContent);
        return (WebTextContent) cell.getContent();
    }

    private WebImage image(WebCell cell) {
        Assert.assertTrue("内容应为图片", cell.getContent() instanceof WebImage);
        return (WebImage) cell.getContent();
    }

    private WebChart chart(WebCell cell) {
        Assert.assertTrue("内容应为图表", cell.getContent() instanceof WebChart);
        return (WebChart) cell.getContent();
    }

    /**
     * (a) 嵌套布局的行列/跨度/取值与 position/measuredSize 一致。
     */
    @Test
    public void nestedLayoutGeometryShouldMatchContainer() {
        VLayout root = new VLayout();
        HLayout row = new HLayout();
        row.addItem(new TextComponent(Size.of(1, 1), "a"));
        row.addItem(new TextComponent(Size.of(1, 1), "b"));
        root.addItem(row);
        root.addItem(new TextComponent(Size.of(2, 1), "c"));

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));

        Assert.assertEquals("列数应为 2", 2, web.getColCount());
        Assert.assertEquals("行数应为 2", 2, web.getRowCount());

        WebCell a = findCell(web, 0, 0);
        Assert.assertNotNull(a);
        Assert.assertEquals("text", a.getContent().getKind());
        Assert.assertEquals("a", text(a).getValue());
        Assert.assertEquals(1, a.getRowSpan());
        Assert.assertEquals(1, a.getColSpan());

        Assert.assertEquals("b", text(findCell(web, 0, 1)).getValue());

        WebCell c = findCell(web, 1, 0);
        Assert.assertNotNull(c);
        Assert.assertEquals("c", text(c).getValue());
        Assert.assertEquals("c 应跨 2 列", 2, c.getColSpan());
    }

    /**
     * (h) SpanComponent 只定义合并区域，无内容。
     */
    @Test
    public void spanCellShouldHaveNoContent() {
        VLayout root = new VLayout();
        root.addItem(new SpanComponent(Size.of(2, 1)));

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));
        WebCell cell = findCell(web, 0, 0);

        Assert.assertNotNull(cell);
        Assert.assertNull("合并区域无内容", cell.getContent());
        Assert.assertEquals(2, cell.getColSpan());
    }

    /**
     * (b) 样式值归一化。
     */
    @Test
    public void styleValueShouldBeNormalized() {
        VLayout root = new VLayout();
        root.addStyle(CellStyles.alignHorizontal, HorizontalAlignment.CENTER);
        root.addStyle(CellStyles.fontBold, true);
        root.addStyle(CellStyles.fontHeight, 13.);
        root.addStyle(CellStyles.fontColor, CellStyles.createColor(0x2570d4));
        root.addItem(new TextComponent(Size.of(1, 1), "v"));

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));
        Map<String, Object> style = findCell(web, 0, 0).getStyle();

        Assert.assertEquals("枚举应转小写", "center", style.get("alignHorizontal"));
        Assert.assertEquals(Boolean.TRUE, style.get("fontBold"));
        Assert.assertEquals(13.0, ((Number) style.get("fontHeight")).doubleValue(), 0.0001);
        Assert.assertEquals("颜色应归一化为 #RRGGBB", "#2570d4", style.get("fontColor"));
    }

    /**
     * (b) 未注册 key 原样保留、null 安全。
     */
    @Test
    public void normalizerShouldKeepUnknownKeyAndHandleNull() {
        StyleMap map = new StyleMap();
        StyleKey<String> key = new StyleKey<>("unknownKey", CellStyle.class, String.class, (t, v) -> {
        });
        map.addStyle(key, "rawValue");

        Assert.assertEquals("rawValue", WebStyleNormalizer.normalize(map).get("unknownKey"));
        Assert.assertTrue("null 应返回空 Map", WebStyleNormalizer.normalize(null).isEmpty());
    }

    /**
     * (c) 父级继承样式经 finalStyle 进入 cell.style。
     */
    @Test
    public void inheritedStyleShouldReachCell() {
        VLayout root = new VLayout();
        root.addStyle(CellStyles.fontBold, true);
        TextComponent text = new TextComponent(Size.of(1, 1), "v");
        text.addStyle(CellStyles.fontHeight, 20.);
        root.addItem(text);

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));
        Map<String, Object> style = findCell(web, 0, 0).getStyle();

        Assert.assertEquals("应继承父级 fontBold", Boolean.TRUE, style.get("fontBold"));
        Assert.assertEquals("应保留自身 fontHeight", 20.0,
                ((Number) style.get("fontHeight")).doubleValue(), 0.0001);
    }

    /**
     * (d) 图片内嵌为可解码的 base64 dataURL，并带上渲染参数。
     */
    @Test
    public void imageShouldBeEmbeddedWithMetadata() {
        VLayout root = new VLayout();
        ImageComponent image = new ImageComponent();
        image.setImage(new Base64Image(PNG_DATA_URL));
        root.addItem(image);

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));
        WebImage content = image(findCell(web, 0, 0));

        Assert.assertEquals("image", content.getKind());
        String dataUrl = content.getDataUrl();
        Assert.assertNotNull("图片应内嵌为 dataURL", dataUrl);
        Assert.assertTrue("dataURL 前缀应正确", dataUrl.startsWith("data:image/png;base64,"));

        String payload = dataUrl.substring("data:image/png;base64,".length());
        Assert.assertTrue("base64 应可解码且非空", Base64.getDecoder().decode(payload).length > 0);

        Assert.assertEquals("缩放类型应为 fit_xy", "fit_xy", content.getScaleType());
        Assert.assertEquals(Double.valueOf(1.0), content.getScaleHeight());
        Assert.assertEquals("2,2,2,2", content.getPadding());
    }

    /**
     * (e) 图表结构化数据：坐标轴、系列值、区域引用均可还原。
     */
    @Test
    public void chartShouldCarryAxesAndData() {
        DefaultChartDataType type = new NoRenderChartType();
        ChartLabelAxis labelAxis = new ChartLabelAxis();
        labelAxis.setData(Arrays.asList("A", "B", "C"));
        type.setLabelAxis(labelAxis);

        ChartValueAxis valueAxis = new ChartValueAxis();
        ChartValueAxisData series = new ChartValueAxisData(new ValueAxisDataResolver(Arrays.asList(1, 2, 3)));
        series.setType(ChartTypes.LINE);
        series.setTitle("S1");
        valueAxis.addData(series);
        // 第二条系列走区域引用（如引用工作表某行）
        ChartValueAxisData refSeries = new ChartValueAxisData(
                new RelationAxisDataResolver(new CellRangeAddress(4, 4, 0, 2)));
        refSeries.setType(ChartTypes.BAR);
        valueAxis.addData(refSeries);
        type.setValueAxis1(valueAxis);

        VLayout root = new VLayout();
        root.addItem(new ChartComponent(type));

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));
        WebChart content = chart(findCell(web, 0, 0));

        Assert.assertEquals("chart", content.getKind());
        Assert.assertTrue("图表类型应含 line", content.getChartTypes().contains("line"));

        Assert.assertNotNull(content.getLabelAxis());
        Assert.assertEquals(Arrays.asList("A", "B", "C"), content.getLabelAxis().getValues());

        Assert.assertEquals(1, content.getValueAxes().size());
        java.util.List<WebChartSeries> seriesList = content.getValueAxes().get(0).getSeries();
        Assert.assertEquals(2, seriesList.size());
        Assert.assertEquals("S1", seriesList.get(0).getTitle());
        Assert.assertEquals("line", seriesList.get(0).getChartType());
        Assert.assertEquals(Arrays.asList(1, 2, 3), seriesList.get(0).getValues());

        WebCellRange range = seriesList.get(1).getReference();
        Assert.assertNotNull("第二条系列应为区域引用", range);
        Assert.assertEquals(4, range.getFirstRow());
        Assert.assertEquals(4, range.getLastRow());
        Assert.assertEquals(0, range.getFirstCol());
        Assert.assertEquals(2, range.getLastCol());
    }

    /**
     * (f) 列宽/行高来自 ReportContext 的已算值。
     */
    @Test
    public void columnWidthAndRowHeightShouldBeExported() {
        VLayout root = new VLayout();
        TextComponent text = new TextComponent(Size.of(2, 1), "v");
        text.addStyle(CellStyles.width, "20");
        text.addStyle(CellStyles.height, "30");
        root.addItem(text);

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));

        Assert.assertEquals("第 0 列列宽应为 20 字符", 20.0, web.getColumnWidths().get(0), 0.0001);
        Assert.assertEquals("第 1 列列宽应为 20 字符", 20.0, web.getColumnWidths().get(1), 0.0001);
        // 框架对显式高度 +2 后再写入
        Assert.assertEquals("第 0 行行高应为 32 磅", 32.0, web.getRowHeights().get(0), 0.0001);
    }

    /**
     * (f) Sheet 级样式用 SheetStyles 注册表归一化（defaultRowHeight 应为 Float 而非字符串）。
     */
    @Test
    public void sheetStyleShouldBeNormalizedWithSheetStylesRegistry() {
        VLayout root = new VLayout();
        root.addItem(new TextComponent(Size.of(1, 1), "v"));

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));

        Object defaultRowHeight = web.getSheetStyle().get("defaultRowHeight");
        Assert.assertTrue("defaultRowHeight 应为数值类型", defaultRowHeight instanceof Number);
        Assert.assertEquals(20.0, ((Number) defaultRowHeight).doubleValue(), 0.0001);
    }

    /**
     * (g) 富文本拆分为分片，value 为纯文本拼接。
     */
    @Test
    public void richTextShouldBeSplitIntoRuns() {
        VLayout root = new VLayout();
        root.addItem(new TextComponent(Size.of(1, 1),
                "<html><font fontBold=\"true\">B</font>plain</html>"));

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(root));
        WebTextContent content = text(findCell(web, 0, 0));

        Assert.assertEquals("value 应为纯文本拼接", "Bplain", content.getValue());
        Assert.assertEquals(2, content.getRichText().size());
        Assert.assertEquals("B", content.getRichText().get(0).getText());
        Assert.assertEquals("分片应带自身样式", Boolean.TRUE, content.getRichText().get(0).getStyle().get("fontBold"));
        Assert.assertEquals("plain", content.getRichText().get(1).getText());
    }

    /**
     * (g) List 纵向展开。
     */
    @Test
    public void verticalListShouldExpandHeaderAndData() {
        ListComponent list = new ListComponent();
        list.setSpan(2);
        ListHeader listHeader = new ListHeader("H", 1);
        listHeader.addStyle(CellStyles.fontBold, true);
        list.setHeader(listHeader);
        list.setDataList(Arrays.asList("x", "y"));
        list.setOrientation(Orientation.VERTICAL);

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(list));

        WebCell header = findCell(web, 0, 0);
        Assert.assertNotNull(header);
        Assert.assertEquals("H", text(header).getValue());
        Assert.assertEquals("表头应跨 2 列", 2, header.getColSpan());
        Assert.assertEquals("类型应取组件类简名", "ListComponent", header.getType());
        // 纵向表头在框架里未叠加 header.getStyle()，回显须保持一致（DEFAULT_STYLE 的 fontBold=false）
        Assert.assertEquals("纵向表头不应叠加 header 样式", Boolean.FALSE, header.getStyle().get("fontBold"));

        WebCell first = findCell(web, 1, 0);
        Assert.assertNotNull(first);
        Assert.assertEquals("x", text(first).getValue());
        Assert.assertEquals(2, first.getColSpan());
        Assert.assertEquals("y", text(findCell(web, 2, 0)).getValue());
    }

    /**
     * (g) List 横向展开。
     */
    @Test
    public void horizontalListShouldExpandHeaderAndData() {
        ListComponent list = new ListComponent();
        list.setSpan(1);
        ListHeader listHeader = new ListHeader("H", 1);
        listHeader.addStyle(CellStyles.fontBold, true);
        list.setHeader(listHeader);
        list.setDataList(Arrays.asList("x", "y"));
        list.setOrientation(Orientation.HORIZONTAL);

        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, export(list));

        WebCell header = findCell(web, 0, 0);
        Assert.assertEquals("H", text(header).getValue());
        // 横向表头在框架里叠加了 header.getStyle()，回显须包含
        Assert.assertEquals("横向表头应叠加 header 样式", Boolean.TRUE, header.getStyle().get("fontBold"));
        Assert.assertEquals("x", text(findCell(web, 0, 1)).getValue());
        Assert.assertEquals("y", text(findCell(web, 0, 2)).getValue());
    }


    /**
     * (g) 空入参安全返回，不抛异常。
     */
    @Test
    public void nullResultShouldReturnEmptyStructure() {
        WebExcel web = BridgeExporter.export(WebJsonExporter.instance, null);

        Assert.assertNotNull(web);
        Assert.assertTrue(web.getCells().isEmpty());
        Assert.assertTrue(web.getColumnWidths().isEmpty());
        Assert.assertTrue(web.getRowHeights().isEmpty());
        Assert.assertEquals(0, web.getRowCount());
        Assert.assertEquals(0, web.getColCount());
    }

    /**
     * 图表类型桩：保留数据但 needCreateChart=false，避免单测触发 POI 图表绘制
     */
    private static class NoRenderChartType extends DefaultChartDataType {
        @Override
        public boolean needCreateChart() {
            return false;
        }
    }

    @After
    public void tearDown() throws Exception {
        report.getWorkbook().close();
    }
}