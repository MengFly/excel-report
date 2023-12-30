package io.github.mengfly.excel.report.template.exepression.process;

import io.github.mengfly.excel.report.template.DataContext;

import java.util.function.Consumer;

public class NoneExpression extends ProcessExpression {
    public NoneExpression(DataContext context, String expression) {
        super(context, expression);
    }

    @Override
    public void process(Consumer<DataContext> dataConsumer) {
        dataConsumer.accept(context);
    }
}
