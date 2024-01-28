package io.github.mengfly.excel.report.template.factory;

import io.github.mengfly.excel.report.exception.ExcelReportException;
import io.github.mengfly.excel.report.exception.TemplateNotFoundException;
import io.github.mengfly.excel.report.template.ReportTemplate;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;

@Data
public class ClasspathTemplateFactory implements TemplateFactory {

    private ClassLoader classLoader;


    public ClasspathTemplateFactory() {
        this(ClasspathTemplateFactory.class.getClassLoader());
    }

    public ClasspathTemplateFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public ReportTemplate getTemplate(String id) throws TemplateNotFoundException {
        id = checkId(id);

        try (InputStream stream = classLoader.getResourceAsStream(id)) {
            if (stream == null) {
                throw new TemplateNotFoundException(id);
            }
            return new ReportTemplate(stream);
        } catch (IOException e) {
            throw new ExcelReportException("create template fail.", e);
        }

    }
}
