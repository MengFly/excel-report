package io.github.mengfly.excel.report.template.exepression.process;

import io.github.mengfly.excel.report.template.DataContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

/**
 * 流程控制表达式基类
 */
@Data
@RequiredArgsConstructor
public abstract class ProcessExpression {
    protected final DataContext context;
    protected final String expression;

    public abstract void process(Consumer<DataContext> dataConsumer);
}
