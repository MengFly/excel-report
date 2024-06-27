package io.github.mengfly.excel.report.template.exepression;

import io.github.mengfly.excel.report.template.DataContext;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.Expression;

import java.util.Optional;

@RequiredArgsConstructor
public class StandardExpression implements TemplateExpression {

    private final Expression expression;


    @Override
    public Object evaluate(DataContext dataContext) {

        final Object value = expression.getValue(getEvaluationContext(dataContext));
        if (value instanceof Optional) {
            // 自动解包Optional, 以这种方式支持懒加载数据
            return ((Optional<?>) value).orElse(null);
        }
        return value;
    }

    @Override
    public String toString() {
        return "exp:" + expression.getExpressionString();
    }
}
