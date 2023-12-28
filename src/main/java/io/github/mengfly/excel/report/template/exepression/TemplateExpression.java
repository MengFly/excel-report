package io.github.mengfly.excel.report.template.exepression;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return true;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) {
            return new TypedValue(((Map<?, ?>) target).get(name));
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return true;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
            //noinspection rawtypes,unchecked
            ((Map) target).put(name, newValue);
        }
    }

}
