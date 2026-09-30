package io.github.mengfly.excel.report.exapmle;

import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.exapmle.util.TestDataUtil;
import io.github.mengfly.excel.report.exapmle.util.TestTemplateUtil;
import io.github.mengfly.excel.report.template.DataContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestTemplate {

    private static ExcelReport report;

    @BeforeClass
    public static void before() {
        report = new ExcelReport();
    }

    @AfterClass
    public static void after() throws IOException {
        report.save(new File("example/test-template.xlsx"));
        // 不要在这里用系统默认程序打开生成的 xlsx：窗口会一直持有文件句柄，
        // 下一次 mvn test 的 save 会因"文件被另一进程占用"失败（CI/无桌面环境也会失败）。
        // 需要肉眼检查时手动打开 example/test-template.xlsx 即可。
    }

    @Test
    public void testTemplate() {
        TestTemplateUtil.exportTemplate(report, new DataContext(), "TestLayoutReport.xml");
        TestTemplateUtil.exportTemplate(report, IndexSensitivityTemplateReportTest.createDataContext(), "IndexSensitivityTemplate.xml");
        TestTemplateUtil.exportTemplate(report, createTemplateContext(), "TestTemplate.xml");
        TestTemplateUtil.exportTemplate(report, new DataContext(), "TestGridLayout.xml");
    }

    private DataContext createTemplateContext() {
        DataContext context = new DataContext();
        context.put("image", TestDataUtil.getTestImageFile());
        context.put("tableData", TestDataUtil.getData(10));
        context.put("listData", TestDataUtil.getRandomStringList(9));
        context.put("base64Image", TestDataUtil.getBase64Image());
        List<String> label = TestDataUtil.getRandomStringList(10);
        context.put("chartLabelData", label);

        List<List<Integer>> testChartData = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            testChartData.add(TestDataUtil.getRandomIntegerList(10));
        }
        context.put("chartValueData", testChartData);
        return context;

    }

}
