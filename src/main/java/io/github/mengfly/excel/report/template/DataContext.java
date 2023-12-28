package io.github.mengfly.excel.report.template;

import io.github.mengfly.excel.report.template.exepression.ExpressionHelper;
import lombok.NonNull;

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Object> data = new HashMap<>();

        public Builder put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Builder putAll(Map<String, Object> data) {
            this.data.putAll(data);
            return this;
        }

        public Builder putAll(DataContext data) {
            this.data.putAll(data);
            return this;
        }


        public DataContext build() {
            return build(new ExpressionHelper());
        }

        public DataContext build(ExpressionHelper expressionHelper) {
            return new DataContext(data, expressionHelper);
        }
    }
}
