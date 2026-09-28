package io.github.mengfly.excel.report.exapmle;

import cn.hutool.core.util.RandomUtil;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.exapmle.util.TestTemplateUtil;
import io.github.mengfly.excel.report.template.DataContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexSensitivityTemplateReportTest {

    @Test
    public void testIndexSensitivityTemplateReport() throws IOException {
        final ExcelReport report = new ExcelReport();
        TestTemplateUtil.exportTemplate(report, createDataContext(), "IndexSensitivityTemplate.xml");
        report.save(new File("example/IndexSensitivityTemplate.xlsx"));
    }


    public static DataContext createDataContext() {
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
