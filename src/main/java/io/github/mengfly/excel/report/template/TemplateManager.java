package io.github.mengfly.excel.report.template;

import io.github.mengfly.excel.report.exception.TemplateNotFoundException;
import io.github.mengfly.excel.report.template.factory.ClasspathTemplateFactory;
import io.github.mengfly.excel.report.template.factory.TemplateFactory;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TemplateManager {

    private static TemplateManager instance;

    public static TemplateManager getInstance() {
        if (instance == null) {
            instance = new TemplateManager();
        }
        return instance;
    
    }

    /**
     * 模板工厂
     */
    private TemplateFactory templateFactory = new ClasspathTemplateFactory();


    /**
     * 获取模板
     * @param id 模板Id
     * @return 模板信息
     * @throws TemplateNotFoundException 如果找不到模板，抛出异常
     */
    public ReportTemplate getTemplate(String id) throws TemplateNotFoundException {
        final ReportTemplate template = templateFactory.getTemplate(id);
        template.setTemplateManager(this);
        return template;
    }


}
