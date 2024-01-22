package io.github.mengfly.excel.report.template.factory;

import io.github.mengfly.excel.report.exception.ExcelReportException;
import io.github.mengfly.excel.report.exception.TemplateNotFoundException;
import io.github.mengfly.excel.report.template.ReportTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Getter
@RequiredArgsConstructor
public class DirTemplateFactory implements TemplateFactory {

    private final File rootDir;


    @Override
    public ReportTemplate getTemplate(String id) throws TemplateNotFoundException {
        id = checkId(id);

        File file = new File(rootDir, id);

        if (file.exists() && file.isFile()) {
            try (FileInputStream stream = new FileInputStream(file)) {
                return new ReportTemplate(stream);
            } catch (IOException e) {
                throw new ExcelReportException("create template fail.", e);
            }
        }

        throw new TemplateNotFoundException(id);
    }
}
