package io.github.mengfly.excel.report.template;

import lombok.NonNull;
import io.github.mengfly.excel.report.template.exepression.ExpressionHelper;

import java.util.HashMap;
import java.util.Map;

public class DataContext extends HashMap<String, Object> {
    private final ExpressionHelper expressionHelper;

    public DataContext() {
        this(null, new ExpressionHelper());
    }

    public DataContext(Map<String, Object> data, @NonNull ExpressionHelper helper) {
        if (data != null) {
            this.putAll(data);
        }
        this.expressionHelper = helper;
    }

    public DataContext createChildContext(Map<String, Object> data) {
        DataContext context = new DataContext(this, this.expressionHelper);
        context.putAll(data);
        return context;
    }


    public Object doExpression(String expression) {
        return expressionHelper.doExpression(expression, this);
    }

    public <T> T doExpression(String expression, Class<T> clazz) {
        return expressionHelper.doExpression(expression, this, clazz);
    }


}
