package io.github.mengfly.excel.report.exapmle;

import io.github.mengfly.excel.report.exapmle.util.TestDataUtil;
import io.github.mengfly.excel.report.exapmle.util.TestTemplateUtil;
import io.github.mengfly.excel.report.template.DataContext;
import org.junit.Test;

import java.io.IOException;

public class SimpleTemplateTest {

    @Test
    public void testGridLayout() throws IOException {
        TestTemplateUtil.export("TestGridLayout.xml", new DataContext());
    }

    @Test
    public void testImage() throws IOException {
        DataContext context = new DataContext();
        context.put("image", TestDataUtil.getTestImageFile());
        TestTemplateUtil.export("TestImageTemplate.xml", context);
    }

    @Test
    public void testLayoutReport() throws IOException {
        TestTemplateUtil.export("TestLayoutReport.xlsx", new DataContext());
    }
}

