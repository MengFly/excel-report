package io.github.mengfly.excel.report.template.expression.process;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
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

    /*
     * 该流程控制所属的模板节点，仅用于渲染失败时给出标签路径；直接 new ProcessControl(expression) 的旧用法为 null。
     */
    private final ContainerTreeNode node;

    public ProcessControl(ProcessExpression expression) {
        this(expression, null);
    }

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
                reportRenderError(dataContext, e);
            }
        });
        return list;

    }

    private void reportRenderError(DataContext context, Exception e) {
        final String controlExpression = expression == null ? null : expression.getExpression();
        if (node != null) {
            node.reportRenderError(context, controlExpression, e);
            return;
        }
        final String expressionPart = StrUtil.isEmpty(controlExpression) ? "" : "；表达式：" + controlExpression;
        log.warn("渲染组件失败，跳过该节点{}", expressionPart, e);
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
        return create(tag, expression, context, null);
    }

    public static ProcessControl create(String tag, String expression, DataContext context, ContainerTreeNode node) {
        ProcessExpression processExpression = getExpression(tag, expression, context);
        return new ProcessControl(processExpression, node);
    }
}
