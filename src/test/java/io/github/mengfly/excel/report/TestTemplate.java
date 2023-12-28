package io.github.mengfly.excel.report;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.report.TestDataUtil;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTemplate {

    private static ExcelReport report;

    @BeforeClass
    public static void before() {
        report = new ExcelReport();
    }

    @AfterClass
    public static void after() throws IOException {
        report.save(new File("test-template.xlsx"));
    }

    @Test
    public void testTemplate() throws IOException {
        exportTemplate(report, new DataContext(), "TestLayoutReport.xml");
        exportTemplate(report, createIndexSensitivityTemplateContext(), "IndexSensitivityTemplate.xml");
        exportTemplate(report, createImageTemplateContext(), "TestImage.xml");
    }

    private DataContext createImageTemplateContext() {
        DataContext context = new DataContext();
        context.put("image", TestDataUtil.getTestImageFile());
        context.put("tableData", TestDataUtil.getData(10));
        context.put("listData", TestDataUtil.getRandomStringList(9));
        return context;
    }


    private void exportTemplate(ExcelReport report, DataContext context, String templatePath) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            ReportTemplate template = new ReportTemplate(stream);
            report.exportTemplate(template, FileUtil.mainName(templatePath), context);
        }
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
