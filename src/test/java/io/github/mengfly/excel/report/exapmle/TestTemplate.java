package io.github.mengfly.excel.report.exapmle;

import io.github.mengfly.excel.report.exapmle.util.TestDataUtil;
import io.github.mengfly.excel.report.exapmle.util.TestTemplateUtil;
import io.github.mengfly.excel.report.template.DataContext;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestTemplate {


    @Test
    public void testTemplate() throws IOException {
        TestTemplateUtil.export("TestTemplate.xml", createTemplateContext());
    }

    public static DataContext createTemplateContext() {
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
