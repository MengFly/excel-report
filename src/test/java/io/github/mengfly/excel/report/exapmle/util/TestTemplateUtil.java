package io.github.mengfly.excel.report.exapmle.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.DesktopUtil;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.excel.ExportResult;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import io.github.mengfly.excel.report.template.TemplateManager;

import java.io.File;
import java.io.IOException;

public class TestTemplateUtil {
    private static final TemplateManager templateManager = new TemplateManager();

    public static void exportTemplate(ExcelReport report, DataContext context, String templatePath) {
        ReportTemplate template = templateManager.getTemplate(templatePath);
        final ExportResult exportResult = report.exportTemplate(template, FileUtil.mainName(templatePath), context);
        System.out.println(exportResult.getContainer().print());
    }

    public static void saveReport(ExcelReport report, String name) throws IOException {
        File file = new File("example/" + name + ".xlsx");
        report.save(file);
        DesktopUtil.open(file);
    }

}
