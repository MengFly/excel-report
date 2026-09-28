package io.github.mengfly.excel.report;

import cn.hutool.core.swing.DesktopUtil;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.report.IndexSensitivityTemplateReportTest;
import io.github.mengfly.excel.report.report.TestDataUtil;
import io.github.mengfly.excel.report.report.util.TestTemplateUtil;
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
        DesktopUtil.open(new File("example/test-template.xlsx"));
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
