package io.github.mengfly.excel.report.excel;

import io.github.mengfly.excel.report.component.TextComponent;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.StyleMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/*
 * ExcelReport 的 Sheet 命名去重规则：同名依次得到 A / A_1 / A_2，匿名（name 为空）得到 sheet / sheet_1 / ...，
 * 且生成的序号名不能与用户自己起的名字撞车。
 *
 * 原实现把"读计数器"的 key（name）与"写计数器"的 key（补过后缀的 sheetName）混用，计数器永远停在 1
 * ⇒ 第 3 个同名 Sheet、或第 2 个匿名 Sheet 必抛 IllegalArgumentException: already contains a sheet named 'X'，
 * 而 exportSheet/exportTemplate 的契约是"名称重复则自动添加序号"。取舍与实测见 doc/优化建议.md 四.4。
 */
public class ExcelReportSheetNameTest {

    private final ExcelReport report = new ExcelReport();

    private void export(String name) {
        report.exportSheet(name, new TextComponent(Size.of(1, 1), "v"), new StyleMap());
    }

    private String[] sheetNames() {
        int count = report.getWorkbook().getNumberOfSheets();
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = report.getWorkbook().getSheetName(i);
        }
        return names;
    }

    /** 同名 3 个及以上：必须依次得到 A / A_1 / A_2，而不是在第 3 个崩。 */
    @Test
    public void repeatedNameShouldKeepAppendingSequence() {
        export("A");
        export("A");
        export("A");
        export("A");

        Assert.assertArrayEquals(new String[]{"A", "A_1", "A_2", "A_3"}, sheetNames());
    }

    /** 匿名 Sheet（name 为 null / 空串）：同样要去重，不能第 2 个就崩。 */
    @Test
    public void anonymousSheetShouldAlsoBeDeduplicated() {
        export(null);
        export(null);
        export("");

        Assert.assertArrayEquals(new String[]{"sheet", "sheet_1", "sheet_2"}, sheetNames());
    }

    /** 生成的序号名不能与用户自己起的名字撞车。 */
    @Test
    public void generatedNameShouldNotCollideWithUserProvidedName() {
        export("A");
        export("A");        // -> A_1
        export("A_1");      // 用户自己起了 A_1 -> 必须是 A_1_1 之类，而不是抛异常

        String[] names = sheetNames();
        Assert.assertEquals(3, names.length);
        Assert.assertEquals("A", names[0]);
        Assert.assertEquals("A_1", names[1]);
        Assert.assertEquals("A_1_1", names[2]);
    }

    /** 不同基名之间互不影响。 */
    @Test
    public void differentBaseNamesShouldNotInterfere() {
        export("A");
        export("B");
        export("A");

        Assert.assertArrayEquals(new String[]{"A", "B", "A_1"}, sheetNames());
    }

    /** 没有任何 Sheet 时保存，{@code save} 的兜底会补一个默认 Sheet（Excel 才能打开）。 */
    @Test
    public void saveShouldCreateFallbackSheetWhenEmpty() throws Exception {
        File file = File.createTempFile("excel-report-empty", ".xlsx");
        try {
            report.save(file);
            Assert.assertArrayEquals(new String[]{"sheet"}, sheetNames());
            Assert.assertTrue(file.length() > 0);
        } finally {
            Assert.assertTrue("临时文件应可删除", file.delete() || !file.exists());
        }
    }

    @After
    public void tearDown() throws Exception {
        report.getWorkbook().close();
    }
}
