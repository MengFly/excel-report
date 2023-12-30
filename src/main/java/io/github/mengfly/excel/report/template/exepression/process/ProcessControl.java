package io.github.mengfly.excel.report.template.exepression.process;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.template.DataContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Data
@Slf4j
@RequiredArgsConstructor
public class ProcessControl {
    private final ProcessExpression expression;

    public <T> T fetchOne(Function<DataContext, T> doProcess) {

        final List<T> list = fetchList(doProcess);
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    public <T> List<T> fetchList(Function<DataContext, T> doProcess) {
        List<T> list = new ArrayList<>();

        onFetch(dataContext -> {
            try {
                list.add(doProcess.apply(dataContext));
            } catch (Exception e) {
                log.error("execute expression fail: {}", expression.getExpression(), e);
            }
        });
        return list;

    }

    public void onFetch(Consumer<DataContext> doProcess) {
        if (expression != null) {
            expression.process(doProcess);
        }
    }


    public static ProcessExpression getExpression(String tag, String expression, DataContext context) {
        if (StrUtil.isEmpty(expression)) {
            return new NoneExpression(context, expression);
        }
        if ("if".equals(tag)) {
            return new IfExpression(context, expression);
        } else if ("for".equals(tag)) {
            return new ForExpression(context, expression);
        }
        return new NoneExpression(context, expression);
    }

    public static ProcessControl create(String tag, String expression, DataContext context) {
        ProcessExpression processExpression = getExpression(tag, expression, context);
        return new ProcessControl(processExpression);
    }
}
