package io.github.mengfly.excel.report.template.expression;

import java.util.List;
import java.util.Map;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import io.github.mengfly.excel.report.template.DataContext;

public interface TemplateExpression {


    Object evaluate(DataContext dataContext);

    default EvaluationContext getEvaluationContext(DataContext dataContext) {
        StandardEvaluationContext context = new StandardEvaluationContext(dataContext);

        context.addPropertyAccessor(new MapAccessor());
        if (dataContext.getRoot() != null) {
            context.addMethodResolver(new RootMethodResolver(dataContext));
        }
        return context;
    }

    /**
     * map 取值访问器。
     * <p>
     * 顶层变量优先从 {@link DataContext} 的 map 中取；map 里没有该键、且上下文设置了 root 时，
     * 再回落到 root 对象上按属性读取（例如 {@code ${title}} 可以命中 root 的 {@code getTitle()}）。
     */
    class MapAccessor implements PropertyAccessor {

        private static final ReflectivePropertyAccessor ROOT_ACCESSOR = new ReflectivePropertyAccessor();

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class[]{Map.class};
        }

        @Override
        public boolean canRead(@NonNull EvaluationContext context,@Nullable Object target, @Nullable String name) {
            return true;
        }

        @Override
        public @NonNull TypedValue read(@NonNull EvaluationContext context, @Nullable Object target, @Nullable String name) {
            final Map<?, ?> map = (Map<?, ?>) target;
            if (map.containsKey(name)) {
                return new TypedValue(map.get(name));
            }
            if (target instanceof DataContext) {
                final Object root = ((DataContext) target).getRoot();
                if (root != null) {
                    return new TypedValue(readRootProperty(context, root, name));
                }
            }
            return new TypedValue(null);
        }

        /**
         * 在 root 上按属性读取；root 本身是 map 时按 key 读取。
         */
        private static Object readRootProperty(EvaluationContext context, Object root, String name) {
            if (root instanceof Map) {
                return ((Map<?, ?>) root).get(name);
            }
            try {
                if (ROOT_ACCESSOR.canRead(context, root, name)) {
                    return ROOT_ACCESSOR.read(context, root, name).getValue();
                }
            } catch (AccessException e) {
                // 读不到就当作没有这个变量，与 map 缺键的表现保持一致
            }
            return null;
        }

        @Override
        public boolean canWrite(@NonNull EvaluationContext context, @Nullable Object target, @Nullable String name) {
            return true;
        }

        @SuppressWarnings({ "null", "rawtypes", "unchecked" })
        @Override
        public void write(
                @NonNull EvaluationContext context,
                @Nullable Object target, @Nullable String name, @Nullable Object newValue) {
            ((Map) target).put(name, newValue);
        }
    }

    /**
     * root 方法解析器。
     * <p>
     * 顶层方法调用（如 {@code ${greet('Tom')}}）先尝试在 root 对象上解析，解析不到再交给默认解析器。
     */
    class RootMethodResolver implements MethodResolver {

        private final DataContext dataContext;

        private final ReflectiveMethodResolver delegate = new ReflectiveMethodResolver();

        RootMethodResolver(DataContext dataContext) {
            this.dataContext = dataContext;
        }

        @Override
        @Nullable
        public MethodExecutor resolve(
                @NonNull EvaluationContext context, @Nullable Object target, @NonNull String name,
                @NonNull List<TypeDescriptor> argumentTypes) throws AccessException {
            final Object root = dataContext.getRoot();
            if (target != dataContext || root == null || root == target) {
                return null;
            }
            final MethodExecutor executor = delegate.resolve(context, root, name, argumentTypes);
            if (executor == null) {
                return null;
            }
            // SpEL 会拿原始根对象（DataContext）作为调用目标，这里替换成 root 再执行
            return (evalContext, targetObject, arguments) -> executor.execute(evalContext, root, arguments);
        }
    }

}
