package io.github.mengfly.excel.report.excel;

import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/*
 * 列宽写入的 255 字符上限保护回归用例（见 ReportContext#applyColumnWidth）。
 * 背景：POI 5.2.5 的 XSSFSheet#setColumnWidth 在 width > 255*256 时抛 IllegalArgumentException，
 * 而写列宽在导出收尾阶段执行，异常会让前面渲染好的整份报表无法保存。
 */
public class ReportContextColumnWidthTest {

    private static final int MAX_COLUMN_WIDTH_UNITS = 255 * 256;

    private final XSSFWorkbook workbook = new XSSFWorkbook();

    private ReportContext newContext() {
        return new ReportContext(workbook, workbook.createSheet("s"));
    }

    /** 长文本 + auto 列宽：实测值超过 255 字符时必须钳制到上限，而不是让导出失败。 */
    @Test
    public void longTextAutoWidthShouldBeClampedTo255() {
        ReportContext context = newContext();
        StyleMap style = new StyleMap();
        style.addStyle(CellStyles.width, "auto");
        context.getCellSpan(new Point(0, 0), Size.of(1, 1), style)
                .setValue(StrUtil.repeat('W', 300));

        context.applyCellWidthHeight(new StyleMap());

        Assert.assertTrue("auto 实测列宽应超过 255 字符，否则本用例没覆盖到超限分支",
                context.getColumnWidths().get(0) > 255);
        Assert.assertEquals(MAX_COLUMN_WIDTH_UNITS, context.getSheet().getColumnWidth(0));
    }

    /** 模板显式把列宽写到 255 字符以上：同样按上限写入。 */
    @Test
    public void explicitWidthAboveLimitShouldBeClampedTo255() {
        ReportContext context = newContext();
        StyleMap style = new StyleMap();
        style.addStyle(CellStyles.width, "300");
        context.getCellSpan(new Point(0, 0), Size.of(1, 1), style).setValue("v");

        context.applyCellWidthHeight(new StyleMap());

        Assert.assertEquals(MAX_COLUMN_WIDTH_UNITS, context.getSheet().getColumnWidth(0));
    }

    /** 正常列宽不受钳制影响。 */
    @Test
    public void widthWithinLimitShouldBeWrittenAsIs() {
        ReportContext context = newContext();
        StyleMap style = new StyleMap();
        style.addStyle(CellStyles.width, "20");
        context.getCellSpan(new Point(0, 0), Size.of(1, 1), style).setValue("v");

        context.applyCellWidthHeight(new StyleMap());

        Assert.assertEquals(20 * 256, context.getSheet().getColumnWidth(0));
    }

    /** 小于 1 的列宽提升到下界，避免写出非法宽度。 */
    @Test
    public void widthBelowMinimumShouldBeRaisedToOne() {
        ReportContext context = newContext();
        StyleMap style = new StyleMap();
        style.addStyle(CellStyles.width, "0");
        context.getCellSpan(new Point(0, 0), Size.of(1, 1), style).setValue("v");

        context.applyCellWidthHeight(new StyleMap());

        Assert.assertEquals(256, context.getSheet().getColumnWidth(0));
    }

    /** 超限列被钳制后，其余列仍按各自宽度正常写入。 */
    @Test
    public void overLimitColumnShouldNotBlockOtherColumns() {
        ReportContext context = newContext();
        StyleMap longStyle = new StyleMap();
        longStyle.addStyle(CellStyles.width, "auto");
        context.getCellSpan(new Point(0, 0), Size.of(1, 1), longStyle)
                .setValue(StrUtil.repeat('W', 300));

        StyleMap normalStyle = new StyleMap();
        normalStyle.addStyle(CellStyles.width, "20");
        context.getCellSpan(new Point(1, 0), Size.of(1, 1), normalStyle).setValue("v");

        context.applyCellWidthHeight(new StyleMap());

        Assert.assertEquals(MAX_COLUMN_WIDTH_UNITS, context.getSheet().getColumnWidth(0));
        Assert.assertEquals(20 * 256, context.getSheet().getColumnWidth(1));
    }

    @After
    public void tearDown() throws Exception {
        workbook.close();
    }
}
