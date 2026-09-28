package io.github.mengfly.excel.report.template.exepression;

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.convert.Convert;
import io.github.mengfly.excel.report.template.DataContext;

public class ExpressionHelper {

    private static final String expressionPrefix = "${";
    private static final String expressionSuffix = "}";

    private static final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 全局共享的表达式缓存
     * <p>
     * 表达式解析结果只依赖表达式字符串：AST 不持有任何模板/数据上下文引用
     * （求值时的 {@code StandardEvaluationContext} 由
     * {@link TemplateExpression#getEvaluationContext(DataContext)} 按次创建），
     * 因此可安全地跨模板、跨上下文共享。
     * <p>
     * 容量按"全应用"维度设置（原为每个上下文一份 256）。
     */
    private static final Cache<String, TemplateExpression> SHARED_EXPRESSION_CACHE = new LRUCache<>(2048);

    private final Cache<String, TemplateExpression> expressionMap;

    /**
     * 使用全局共享缓存
     */
    public ExpressionHelper() {
        this(SHARED_EXPRESSION_CACHE);
    }

    /**
     * 使用指定缓存，便于子类隔离或测试注入
     *
     * @param expressionMap 表达式缓存，不可为 null
     */
    protected ExpressionHelper(Cache<String, TemplateExpression> expressionMap) {
        this.expressionMap = expressionMap;
    }

    public Object doExpression(String expression, DataContext dataContext) {
        final TemplateExpression templateExpression = expressionMap.get(expression, true,
                () -> {
                    TemplateExpression exp = createExpression(expression);
                    onCreateExpression(exp);
                    return exp;
                });

        return templateExpression.evaluate(dataContext);
    }

    /**
     * 表达式创建回调，仅在缓存未命中、表达式首次被解析时触发一次
     * <p>
     * ⚠️ 缓存默认全局共享，因此该回调只会由"首次解析该表达式"的那个 {@link ExpressionHelper} 实例收到。
     * 不要在此处挂载与实例相关的状态（例如自定义的 PropertyAccessor 标识），否则其他实例复用缓存项时行为不一致。
     *
     * @param expression 新建的表达式
     */
    protected void onCreateExpression(TemplateExpression expression) {

    }

    /**
     * 清空全局共享的表达式缓存
     * <p>
     * 表达式语义只依赖字符串，通常无需清理；主要用于测试隔离。
     */
    public static void clearSharedCache() {
        SHARED_EXPRESSION_CACHE.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> T doExpression(String expression, DataContext dataContext, Class<T> clazz) {
        final Object evaluate = doExpression(expression, dataContext);
        if (clazz == Object.class) {
            return (T) evaluate;
        }
        return Convert.convert(clazz, evaluate);
    }

    @SuppressWarnings("null")
    public static TemplateExpression createExpression(String expression) {
        List<TemplateExpression> expressions = new ArrayList<>();

        // 开始解析表达式列表
        int startSearch = 0;
        int expressionStart;
        while ((expressionStart = expression.indexOf(expressionPrefix, startSearch)) != -1) {
            if (startSearch != expressionStart) {
                final String subExpression = expression.substring(startSearch, expressionStart);
                expressions.add(new NotExpression(subExpression));
                startSearch = expressionStart;
            }

            // 查询结束位置
            final int end = searchEnd(expression, expressionStart);
            if (end > 0) {
                // 判断是否是表达式
                final String suffix = expression.substring(end, end + 1);
                final String prefix = expression.substring(expressionStart, expressionStart + expressionPrefix.length());
                if (suffix.equals(expressionSuffix) && prefix.equals(expressionPrefix)) {
                    final String subExpression = expression.substring(expressionStart + expressionPrefix.length(), end);
                    try {
                        expressions.add(new StandardExpression(parser.parseExpression(subExpression)));
                    } catch (Exception e) {
                        expressions.add(new NotExpression(expression.substring(expressionStart, end)));
                    }
                } else {
                    expressions.add(new NotExpression(expression.substring(expressionStart, end)));
                }
                startSearch = end + 1;
            } else {
                // 说明搜索到头了
                break;
            }
        }
        if (startSearch < expression.length()) {
            expressions.add(new NotExpression(expression.substring(startSearch)));
        }


        if (expressions.size() == 1) {
            return expressions.get(0);
        }

        return new ComposeExpression(expressions);
    }

    private static int searchEnd(String expression, int expressionStart) {
        int nextStart = expression.indexOf(expressionPrefix, expressionStart + 1);
        if (nextStart == -1) {
            nextStart = expression.length();
        }
        int end = expression.indexOf(expressionSuffix, expressionStart);
        if (end > nextStart) {
            return nextStart - 1;
        }
        if (end == -1) {
            return -1;
        }
        while (true) {
            int next = expression.indexOf(expressionSuffix, end + 1);
            if (next > nextStart || next == -1) {
                return end;
            }
            end = next;
        }
    }
}
