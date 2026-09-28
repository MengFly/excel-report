package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import io.github.mengfly.excel.report.template.TemplateManager;

import java.util.List;
import java.util.Map;

public class IncludeParser extends ContainerParser {


    @Override
    public String getTagName() {
        return "include";
    }

    @Override
    protected List<String> getIgnoreProperties() {
        return super.getIgnoreProperties();
    }

    @Override
    protected Container parse(ContainerTreeNode node, DataContext context) {

        final String attribute = node.getAttribute("ref");
        final ReportTemplate template = TemplateManager.getInstance().getTemplate(attribute);

        final Map<String, String> attributeMap = node.getAttributeMap("ref");

        DataContext includeContext = new DataContext();

        for (String key : attributeMap.keySet()) {
            includeContext.put(key, context.doExpression(attributeMap.get(key)));
        }

        return template.render(includeContext);
    }
}
