package io.github.mengfly.excel.report.template.expression.process;

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

            /*
             * 2026-10-27：求值为 null（键缺失同样得到 null）时不再用父上下文渲染一次循环体。
             * 旧行为会让循环体里的 ${item.xxx} 以 null 求值，报出与数据无关的异常；只摆文字时则多出一行空白，
             * 与空列表的 0 行表现不一致。这里和空列表对齐：不渲染，只留一条告警。
             */
            if (target == null) {
                log.warn("for 表达式 [{}] 求值为 null，跳过循环体", targetObjExpression);
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
                log.warn("for 表达式 [{}] 的求值结果不是集合（{}），跳过循环体", targetObjExpression,
                        target.getClass().getName());
            }
        } catch (Exception e) {
            // 求值异常也不再回退渲染一次：父上下文里没有循环变量，只会再抛一条无关错误
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
