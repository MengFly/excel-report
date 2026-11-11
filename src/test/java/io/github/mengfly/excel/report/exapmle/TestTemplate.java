package io.github.mengfly.excel.report.exapmle;

import io.github.mengfly.excel.report.exapmle.util.TestDataUtil;
import io.github.mengfly.excel.report.exapmle.util.TestTemplateUtil;
import io.github.mengfly.excel.report.template.DataContext;
import lombok.Data;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestTemplate {


    @Test
    public void testTemplate() throws IOException {
        DataContext templateContext = createTemplateContext();
        TestTemplateUtil.export("TestTemplate.xml", templateContext);
    }

    public static DataContext createTemplateContext() {
        DataContext context = new DataContext();

        TemplateData data = new TemplateData();
        data.setImage(TestDataUtil.getTestImageFile());
        data.setTableData(TestDataUtil.getData(10));
        data.setListData(TestDataUtil.getRandomStringList(9));
        data.setBase64Image(TestDataUtil.getBase64Image());
        List<String> label = TestDataUtil.getRandomStringList(10);
        data.setChartLabelData(label);

        List<List<Integer>> testChartData = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            testChartData.add(TestDataUtil.getRandomIntegerList(10));
        }
        data.setChartValueData(testChartData);
        context.setRoot(data);
        return context;
    }

    @Data
    public static class TemplateData {
        private File image;
        private List<Object> tableData;
        private List<String> listData;
        private String base64Image;
        private List<String> chartLabelData;
        private List<List<Integer>> chartValueData;
    }

}
