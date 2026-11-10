package io.github.mengfly.excel.report.template;

import io.github.mengfly.excel.report.template.expression.ExpressionHelper;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class DataContext extends HashMap<String, Object> {
    private final ExpressionHelper expressionHelper;

    /*
     * 已报告过渲染失败的节点。for 循环里同一节点会反复渲染、反复失败，同一个错误只报一次。
     * 作为一次导出内的共享状态随子上下文传递（见构造方法），调用方每次导出新建 DataContext 即自然重置。
     */
    private final Set<ContainerTreeNode> renderWarnedNodes;

    /*
     * 根对象。表达式里取不到的变量和方法会回落到该对象上解析（见 TemplateExpression.MapAccessor）。
     * 随子上下文传递：子上下文除了多出循环变量外，其余查找行为与父上下文一致。
     */
    private Object root;

    public DataContext() {
        this(null, new ExpressionHelper());
    }

    public DataContext(Map<String, Object> data, @NonNull ExpressionHelper helper) {
        if (data != null) {
            this.putAll(data);
        }
        this.expressionHelper = helper;
        this.renderWarnedNodes = data instanceof DataContext
                ? ((DataContext) data).renderWarnedNodes
                : Collections.newSetFromMap(new IdentityHashMap<ContainerTreeNode, Boolean>());
        this.root = data instanceof DataContext ? ((DataContext) data).getRoot() : null;
    }

    /**
     * 根对象：表达式里在 map 中取不到的变量与方法，会继续在该对象上按属性 / 方法解析。
     *
     * @return 根对象，未设置时为 {@code null}
     */
    public Object getRoot() {
        return root;
    }

    /**
     * 设置根对象。map 中已有的同名键优先，取不到时才回落到根对象。
     *
     * @param root 根对象，{@code null} 表示不启用回落
     * @return 当前上下文，便于链式调用
     */
    public DataContext setRoot(Object root) {
        this.root = root;
        return this;
    }

    public DataContext createChildContext(Map<String, Object> data) {
        DataContext context = new DataContext(this, this.expressionHelper);
        context.putAll(data);
        return context;
    }

    /**
     * 标记该模板节点已报告过渲染失败告警，用于同一次导出内按节点去重。
     *
     * @param node 渲染失败的模板节点
     * @return 本次调用是首次标记时返回 {@code true}；该节点已报告过时返回 {@code false}
     */
    public boolean markRenderWarned(ContainerTreeNode node) {
        return renderWarnedNodes.add(node);
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
        private Object root;

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

        /**
         * 设置根对象，语义同 {@link DataContext#setRoot(Object)}。
         */
        public Builder root(Object root) {
            this.root = root;
            return this;
        }


        public DataContext build() {
            return build(new ExpressionHelper());
        }

        public DataContext build(ExpressionHelper expressionHelper) {
            return new DataContext(data, expressionHelper).setRoot(root);
        }
    }
}
