package io.github.mengfly.excel.report.template.exepression;

import lombok.RequiredArgsConstructor;
import io.github.mengfly.excel.report.template.DataContext;

@RequiredArgsConstructor
public class NotExpression implements TemplateExpression {
    private final String expression;


    @Override
    public Object evaluate(DataContext dataContext) {
        return expression;
    }
}
