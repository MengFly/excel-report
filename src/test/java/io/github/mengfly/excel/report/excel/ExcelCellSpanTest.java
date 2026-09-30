package io.github.mengfly.excel.report.excel;

import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/*
 * ExcelCellSpan 的"区域内行 / cell 与样式无关地被建好"这条不变量的回归用例。断言三件事：
 *   1. 区域内的行与 cell 必须建好，且与 style 是否为 null 无关；
 *   2. 样式非空时，区域内每个 cell 都挂上该样式；
 *   3. cellSpanDimension() 读行高（行缺失会 NPE），因此样式为 null 时也不能崩。
 *
 * 之所以要钉住它：构造器那遍"建 cell"的遍历与 setStyle 的遍历逐格重复，看着能合并成一遍省开销，
 * 但实测收益在噪声内（n=20 万 span，best-of-5：457ms vs 447/457ms），而合并后第 1 条就会失效。
 * 取舍过程见 doc/优化建议.md 三.5。
 */
public class ExcelCellSpanTest {

    private final XSSFWorkbook workbook = new XSSFWorkbook();

    private ReportContext newContext() {
        return new ReportContext(workbook, workbook.createSheet("s"));
    }

    @Test
    public void setStyleShouldCreateEveryCellInRegion() {
        ReportContext context = newContext();
        StyleMap style = new StyleMap();
        style.addStyle(CellStyles.borderTop, BorderStyle.THIN);

        ExcelCellSpan span = context.getCellSpan(new Point(2, 3), Size.of(3, 2), style);

        assertWholeRegionExists(context.getSheet(), 3, 2, 2, 3);
        assertRegionStyled(context.getSheet(), 3, 2, 2, 3, span);
    }

    /**
     * 关键的一条：{@code style == null} 时整个区域仍必须被建出来。
     * <p>
     * 这正是"把构造器那遍遍历合并进 {@code setStyle} 以省一遍开销"会踩坏的地方 ——
     * 合并后建 cell 就跟着 {@code style != null} 走了，本用例会立刻失败。
     */
    @Test
    public void nullStyleShouldStillCreateCellsAndNotBreakDimension() {
        ReportContext context = newContext();
        ExcelCellSpan span = new ExcelCellSpan(context, new Point(0, 0), Size.of(2, 3));
        span.setStyle(null, null);

        assertWholeRegionExists(context.getSheet(), 2, 3, 0, 0);

        // cellSpanDimension() 读 sheet.getRow(row).getHeightInPoints()：行缺失就会 NPE
        Assert.assertNotNull("样式为 null 时也不能让 cellSpanDimension() 崩", span.cellSpanDimension());
    }

    /** 只 merge、从不 setValue 的区域（SpanComponent / ChartComponent 那种）也要能收尾。 */
    @Test
    public void mergeOnlySpanShouldSurviveApplyCellWidthHeight() {
        ReportContext context = newContext();
        StyleMap style = new StyleMap();
        style.addStyle(CellStyles.width, "auto");

        context.getCellSpan(new Point(0, 0), Size.of(2, 2), style).merge();
        context.applyCellWidthHeight(new StyleMap());

        assertWholeRegionExists(context.getSheet(), 2, 2, 0, 0);
        Assert.assertEquals("应为一块 2x2 合并区", 1, context.getSheet().getMergedRegions().size());
    }

    /** 1x1 的普通单元格：setStyle + setValue 之后既存在又有值。 */
    @Test
    public void singleCellSpanShouldHaveValueAndStyle() {
        ReportContext context = newContext();
        StyleMap style = new StyleMap();
        style.addStyle(CellStyles.borderLeft, BorderStyle.THIN);

        ExcelCellSpan span = context.getCellSpan(new Point(0, 0), Size.of(1, 1), style);
        span.setValue("v");

        Cell cell = context.getSheet().getRow(0).getCell(0);
        Assert.assertNotNull("左上角 cell 应存在", cell);
        Assert.assertEquals("v", cell.getStringCellValue());
        Assert.assertNotEquals("cell 应挂上真正的样式（而不是默认样式 0）", 0, cell.getCellStyle().getIndex());
    }

    // ------------------------------------------------------------------ helpers

    private static void assertWholeRegionExists(Sheet sheet, int width, int height, int x, int y) {
        for (int i = 0; i < height; i++) {
            Row row = sheet.getRow(y + i);
            Assert.assertNotNull("行 " + (y + i) + " 应存在（cell 建了行就该在）", row);
            for (int j = 0; j < width; j++) {
                Assert.assertNotNull("cell(" + (y + i) + "," + (x + j) + ") 应存在", row.getCell(x + j));
            }
        }
    }

    private void assertRegionStyled(Sheet sheet, int width, int height, int x, int y, ExcelCellSpan span) {
        short expected = -1;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                short index = sheet.getRow(y + i).getCell(x + j).getCellStyle().getIndex();
                if (expected < 0) {
                    expected = index;
                    Assert.assertTrue("样式索引不应为默认 0（说明样式没落上）", index != 0);
                }
                Assert.assertEquals("区域内样式应一致", expected, index);
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        workbook.close();
    }
}
