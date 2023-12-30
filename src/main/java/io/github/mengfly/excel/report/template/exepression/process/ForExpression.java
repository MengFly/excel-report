package io.github.mengfly.excel.report.template.exepression.process;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.util.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class ForExpression extends ProcessExpression {

    private final String arg1;
    private final String arg2;
    private String targetObjExpression;

    public ForExpression(DataContext context, String expression) {
        super(context, expression);

        // 开始解析参数与表达式

        if (expression != null) {

            final int firstIndex = expression.indexOf(':');

            if (firstIndex > 0) {
                this.targetObjExpression = StrUtil.trim(expression.substring(firstIndex + 1));

                String args = expression.substring(0, firstIndex);
                final List<String> argList = StrUtil.splitTrim(args, ',');
                argList.removeIf(StrUtil::isEmpty);
                arg1 = CollectionUtil.get(argList, 0);
                arg2 = CollectionUtil.get(argList, 1);

            } else {
                arg1 = null;
                arg2 = null;
            }
        } else {
            arg1 = null;
            arg2 = null;
        }
    }

    @Override
    public void process(Consumer<DataContext> dataConsumer) {
        if ((arg1 == null && arg2 == null) || StrUtil.isEmpty(targetObjExpression)) {
            dataConsumer.accept(context);
            return;
        }

        try {
            final Object target = context.doExpression(targetObjExpression);

            if (target == null) {
                dataConsumer.accept(context);
                return;
            }

            if (target instanceof Map) {
                if (arg2 == null) {
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) target).entrySet()) {
                        dataConsumer.accept(createForEnv(entry, null));
                    }
                } else {
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) target).entrySet()) {
                        dataConsumer.accept(createForEnv(entry.getKey(), entry.getValue()));
                    }
                }
            } else if (BeanUtil.isList(target)) {
                final List<?> objects = BeanUtil.objectToList(target);
                for (int i = 0; i < objects.size(); i++) {
                    dataConsumer.accept(createForEnv(objects.get(i), i));
                }
            } else {
                log.error("Un support target type {}", target.getClass());
                dataConsumer.accept(context);
            }
        } catch (Exception e) {
            dataConsumer.accept(context);
            log.error("Can't eval expression : {}", targetObjExpression, e);
        }

    }

    private DataContext createForEnv(Object arg1, Object arg2) {
        Map<String, Object> env = new HashMap<>();

        if (this.arg1 != null) {
            env.put(this.arg1, arg1);
        }
        if (this.arg2 != null) {
            env.put(this.arg2, arg2);
        }
        return context.createChildContext(env);
    }
}
