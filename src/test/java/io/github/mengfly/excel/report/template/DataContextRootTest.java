package io.github.mengfly.excel.report.template;

import io.github.mengfly.excel.report.template.expression.ExpressionHelper;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataContextRootTest {

    @Test
    public void testRootFallback() {
        DataContext context = new DataContext();
        context.put("title", "map-title");
        context.setRoot(new RootBean("root-title", "Alice"));

        // map 中已有的键优先
        assertEquals("map-title", context.doExpression("${title}"));
        // map 中没有的变量回落到 root 的 getter
        assertEquals("root-title", context.doExpression("${subtitle}"));
        assertEquals("Alice", context.doExpression("${owner.name}"));
        // 方法也从 root 上解析
        assertEquals("hi Bob", context.doExpression("${greet('Bob')}"));
        // 组合表达式同样生效
        assertEquals("root-title/Alice", context.doExpression("${subtitle}/${owner.name}"));
    }

    @Test
    public void testChildContextInheritsRoot() {
        DataContext context = new DataContext();
        context.setRoot(new RootBean("root-title", "Alice"));

        DataContext child = context.createChildContext(Collections.<String, Object>singletonMap("index", 3));
        assertEquals(3, child.doExpression("${index}"));
        assertEquals("root-title", child.doExpression("${subtitle}"));
    }

    @Test
    public void testBuilderRoot() {
        DataContext context = DataContext.builder()
                .put("title", "map-title")
                .root(new RootBean("root-title", "Alice"))
                .build();

        assertEquals("map-title", context.doExpression("${title}"));
        assertEquals("root-title", context.doExpression("${subtitle}"));
    }

    @Test
    public void testMapRoot() {
        Map<String, Object> root = new HashMap<>();
        root.put("subtitle", "map-root");

        DataContext context = new DataContext();
        context.setRoot(root);

        assertEquals("map-root", context.doExpression("${subtitle}"));
    }

    @Test
    public void testNoRootKeepsOldBehavior() {
        DataContext context = new DataContext();
        context.put("title", "map-title");

        assertEquals("map-title", context.doExpression("${title}"));
        assertNull(context.doExpression("${subtitle}"));

        ExpressionHelper.clearSharedCache();
    }

    public static class RootBean {
        private final String subtitle;
        private final Person owner;

        public RootBean(String subtitle, String ownerName) {
            this.subtitle = subtitle;
            this.owner = new Person(ownerName);
        }

        public String getSubtitle() {
            return subtitle;
        }

        public Person getOwner() {
            return owner;
        }

        public String greet(String name) {
            return "hi " + name;
        }
    }

    public static class Person {
        private final String name;

        public Person(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
