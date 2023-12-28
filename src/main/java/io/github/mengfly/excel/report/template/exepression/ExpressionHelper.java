package io.github.mengfly.excel.report.template.exepression;

import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.convert.Convert;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import io.github.mengfly.excel.report.template.DataContext;

import java.util.ArrayList;
import java.util.List;

public class ExpressionHelper {

    private static final String expressionPrefix = "${";
    private static final String expressionSuffix = "}";

    private final ExpressionParser parser = new SpelExpressionParser();
    private final Cache<String, TemplateExpression> expressionMap = new LRUCache<>(256);

    public Object doExpression(String expression, DataContext dataContext) {
        final TemplateExpression templateExpression = expressionMap.get(expression, true,
                () -> createExpression(expression));

        return templateExpression.evaluate(dataContext);

    }

    public <T> T doExpression(String expression, DataContext dataContext, Class<T> clazz) {
        final Object evaluate = doExpression(expression, dataContext);
        return Convert.convert(clazz, evaluate);
    }

    private TemplateExpression createExpression(String expression) {
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
            final int end = expression.indexOf(expressionSuffix, expressionStart);
            if (end > 0) {
                final String subExpression = expression.substring(expressionStart + expressionPrefix.length(), end);
                expressions.add(new StandardExpression(parser.parseExpression(subExpression)));
                startSearch = end + 1;
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
}
