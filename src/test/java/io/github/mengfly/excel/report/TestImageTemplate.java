package io.github.mengfly.excel.report;

import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.report.TestDataUtil;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.TemplateManager;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestImageTemplate {

    @Test
    public void testImage() throws IOException {
        ExcelReport report = new ExcelReport();

        DataContext context = new DataContext();
        context.put("image", TestDataUtil.getTestImageFile());
        report.exportTemplate(TemplateManager.getInstance().getTemplate("TestImageTemplate"), "testImage", context);

        report.save(new File("test-image.xlsx"));

    }
}
