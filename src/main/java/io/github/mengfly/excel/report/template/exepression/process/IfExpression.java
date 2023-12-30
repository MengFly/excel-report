package io.github.mengfly.excel.report.template.exepression.process;

import io.github.mengfly.excel.report.template.DataContext;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class IfExpression extends ProcessExpression {
    public IfExpression(DataContext context, String expression) {
        super(context, expression);
    }

    @Override
    public void process(Consumer<DataContext> dataConsumer) {
        boolean isTrue = true;
        try {
            isTrue = context.doExpression(expression, Boolean.class);
        } catch (Exception e) {
            log.error("Can't eval expression for: {}", expression);
        }
        if (isTrue) {
            dataConsumer.accept(context);
        }
    }
}
