package io.github.mengfly.excel.report.template.factory;

import io.github.mengfly.excel.report.exception.TemplateNotFoundException;
import io.github.mengfly.excel.report.template.ReportTemplate;

/**
 * 模板工厂
 */
public interface TemplateFactory {

    /**
     * 根据名称获取模板
     * @param id 模板名称
     * @return 模板
     */
    ReportTemplate getTemplate(String id) throws TemplateNotFoundException;


    /**
     * 校验模板Id不能为空
     * @param id 模板Id
     */
    default String checkId(String id) {
        if (id == null || id.isEmpty()) {
            throw new TemplateNotFoundException("Template id is null or empty.");
        }
        if (id.toLowerCase().endsWith(".xml")) {
            return id;
        } else {
            return id + ".xml";
        }
    }
}
