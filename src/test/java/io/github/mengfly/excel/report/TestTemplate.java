package io.github.mengfly.excel.report;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.report.TestDataUtil;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import io.github.mengfly.excel.report.template.TemplateManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTemplate {

    private static ExcelReport report;
    private static final TemplateManager templateManager = new TemplateManager();

    @BeforeClass
    public static void before() {
        report = new ExcelReport();
    }

    @AfterClass
    public static void after() throws IOException {
        report.save(new File("test-template.xlsx"));
    }

    @Test
    public void testTemplate() {
        exportTemplate(report, new DataContext(), "TestLayoutReport.xml");
        exportTemplate(report, createIndexSensitivityTemplateContext(), "IndexSensitivityTemplate.xml");
        exportTemplate(report, createTemplateContext(), "TestTemplate.xml");
    }

    private DataContext createTemplateContext() {
        DataContext context = new DataContext();
        context.put("image", TestDataUtil.getTestImageFile());
        context.put("tableData", TestDataUtil.getData(10));
        context.put("listData", TestDataUtil.getRandomStringList(9));

        List<String> label = TestDataUtil.getRandomStringList(10);
        context.put("chartLabelData", label);

        List<List<Integer>> testChartData = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            testChartData.add(TestDataUtil.getRandomIntegerList(10));
        }
        context.put("chartValueData", testChartData);
        return context;

    }


    private void exportTemplate(ExcelReport report, DataContext context, String templatePath) {
        ReportTemplate template = templateManager.getTemplate(templatePath);
        report.exportTemplate(template, FileUtil.mainName(templatePath), context);
    }

    private static DataContext createIndexSensitivityTemplateContext() {
        final DataContext dataContext = new DataContext();

        List<Map<String, Object>> dataList = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("seq", i + 1);
            data.put("indexName", "测试指标：" + RandomUtil.randomString(5));
            data.put("totalDayCount", 50);
            data.put("availableDayCount", RandomUtil.randomInt(50));
            data.put("minDayIndex", RandomUtil.randomInt(50));
            data.put("maxDayIndex", RandomUtil.randomInt(50));
            data.put("avgDayCount", RandomUtil.randomInt(50));
            data.put("sensitivity", RandomUtil.randomDouble());
            dataList.add(data);
        }
        dataContext.put("areaName", "测试工作面");
        dataContext.put("dataList", dataList);
        return dataContext;
    }

}
