package io.github.mengfly.excel.report.template.exepression;

import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.convert.Convert;
import io.github.mengfly.excel.report.template.DataContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.ArrayList;
import java.util.List;

public class ExpressionHelper {

    private static final String expressionPrefix = "${";
    private static final String expressionSuffix = "}";

    private static final ExpressionParser parser = new SpelExpressionParser();
    private final Cache<String, TemplateExpression> expressionMap = new LRUCache<>(256);

    public Object doExpression(String expression, DataContext dataContext) {
        final TemplateExpression templateExpression = expressionMap.get(expression, true,
                () -> createExpression(expression));

        return templateExpression.evaluate(dataContext);
    }

    @SuppressWarnings("unchecked")
    public <T> T doExpression(String expression, DataContext dataContext, Class<T> clazz) {
        final Object evaluate = doExpression(expression, dataContext);
        if (clazz == Object.class) {
            return (T) evaluate;
        }
        return Convert.convert(clazz, evaluate);
    }

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
