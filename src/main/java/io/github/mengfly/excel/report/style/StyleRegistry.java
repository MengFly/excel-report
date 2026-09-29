package io.github.mengfly.excel.report.style;

import io.github.mengfly.excel.report.style.key.StyleKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 样式 key 的注册表：{@link CellStyles} 与 {@link SheetStyles} 共用的登记 / 查找逻辑。
 * <p>
 * <b>两个门面各持一份实例，不合并为同一张表</b>：它们的 key 集分别镜像 XSD 的 {@code Style} 与 {@code SheetStyle}
 * 两套互斥的命名空间（两类型的子元素交集为空）。并表会让"key 写错上下文"从"查不到 ⇒ 丢弃"变成
 * "查到 ⇒ 进入 {@link StyleMap} ⇒ 事后被目标类型过滤静默丢弃"——表观行为相同，但 {@link StyleMap} 的内容变了，
 * 会连带改变 {@code ReportContext#cellStylePool} / {@code fontPool} 这类以样式表为 key 的池命中。
 */
final class StyleRegistry {

    private final Map<String, StyleKey<?>> styleMap = new HashMap<>();

    <T> StyleKey<T> register(StyleKey<T> key) {
        styleMap.put(key.getId(), key);
        return key;
    }

    <T, S> StyleKey<S> register(String id, Class<T> targetType, Class<S> styleType, BiConsumer<T, S> applier) {
        return register(new StyleKey<>(id, targetType, styleType, applier));
    }

    @SuppressWarnings("unchecked")
    <T> StyleKey<T> getStyleKey(String id) {
        return (StyleKey<T>) styleMap.get(id);
    }

    Collection<StyleKey<?>> keys() {
        return styleMap.values();
    }


    public <T> T initStyle(T target, StyleMap styleMap) {
        if (target == null) {
            return null;
        }
        for (StyleKey<?> key : keys()) {
            if (key.isSupportApplyTarget(target)) {
                styleMap.getStyle(key).ifPresent(style -> key.applyStyle(target, style));
            }
        }
        return target;
    }

}
