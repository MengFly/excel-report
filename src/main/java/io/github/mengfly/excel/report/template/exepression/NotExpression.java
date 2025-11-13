package io.github.mengfly.excel.report.template.exepression;

import io.github.mengfly.excel.report.template.DataContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotExpression implements TemplateExpression {
    private final String expression;


    @Override
    public Object evaluate(DataContext dataContext) {
        return expression;
    }

    @Override
    public String toString() {
        return  expression;
    }
}
