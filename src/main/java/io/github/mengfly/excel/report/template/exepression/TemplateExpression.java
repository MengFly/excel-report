package io.github.mengfly.excel.report.template.exepression;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import io.github.mengfly.excel.report.template.DataContext;

import java.util.Map;

public interface TemplateExpression {


    Object evaluate(DataContext dataContext);

    default EvaluationContext getEvaluationContext(DataContext dataContext) {
        StandardEvaluationContext context = new StandardEvaluationContext(dataContext);

        context.addPropertyAccessor(new MapAccessor());
        return context;
    }

    class MapAccessor implements PropertyAccessor {

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class[]{Map.class};
        }

        @Override
        public boolean canRead(@NonNull EvaluationContext context,@Nullable Object target, @Nullable String name) {
            return true;
        }

        @SuppressWarnings("null")
        @Override
        public @NonNull TypedValue read(@NonNull EvaluationContext context, @Nullable Object target, @Nullable String name) {
            return new TypedValue(((Map<?, ?>) target).get(name));
        }

        @Override
        public boolean canWrite(@NonNull EvaluationContext context, @Nullable Object target, @Nullable String name) {
            return true;
        }

        @SuppressWarnings("null")
        @Override
        public void write(
                @NonNull EvaluationContext context,
                @Nullable Object target, @Nullable String name, @Nullable Object newValue) {
            //noinspection rawtypes,unchecked
            ((Map) target).put(name, newValue);
        }
    }

}
