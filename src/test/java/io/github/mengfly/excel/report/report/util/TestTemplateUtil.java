package io.github.mengfly.excel.report.report.util;

import cn.hutool.core.io.FileUtil;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import io.github.mengfly.excel.report.template.TemplateManager;

public class TestTemplateUtil {
    private static final TemplateManager templateManager = new TemplateManager();

    public static void exportTemplate(ExcelReport report, DataContext context, String templatePath) {
        ReportTemplate template = templateManager.getTemplate(templatePath);
        report.exportTemplate(template, FileUtil.mainName(templatePath), context);
    }

}
