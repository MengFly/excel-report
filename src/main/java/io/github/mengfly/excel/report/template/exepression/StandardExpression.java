package io.github.mengfly.excel.report.template.exepression;

import lombok.RequiredArgsConstructor;
import org.springframework.expression.Expression;
import io.github.mengfly.excel.report.template.DataContext;

@RequiredArgsConstructor
public class StandardExpression implements TemplateExpression {

    private final Expression expression;


    @Override
    public Object evaluate(DataContext dataContext) {

        return expression.getValue(getEvaluationContext(dataContext));
    }
}
