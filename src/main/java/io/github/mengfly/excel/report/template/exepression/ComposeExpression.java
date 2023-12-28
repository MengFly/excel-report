package io.github.mengfly.excel.report.template.exepression;

import io.github.mengfly.excel.report.template.DataContext;

import java.util.List;

public class ComposeExpression implements TemplateExpression {

    private final List<TemplateExpression> expressions;

    public ComposeExpression(List<TemplateExpression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public Object evaluate(DataContext dataContext) {
        StringBuilder sb = new StringBuilder();
        for (TemplateExpression expression : expressions) {
            sb.append(expression.evaluate(dataContext));
        }

        return sb.toString();
    }
}
