package io.github.mengfly.excel.report.style;

import io.github.mengfly.excel.report.style.key.StyleKey;
import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 样式字典
 */
@Getter
public class StyleMap {
    private final Map<String, String> styleMap = new HashMap<>();

    /**
     * 添加样式
     * @param key 样式Key
     * @param value 样式值
     * @param <T> 样式值类型
     */
    public <T> void addStyle(StyleKey<T> key, T value) {
        styleMap.put(key.getId(), key.toString(value));
    }

    /**
     * 按给定的 key 集合摘取样式，作为样式池（如 {@code ReportContext#fontPool}）的 key。
     * <p>
     * <b>缺失的样式直接跳过，不写 null 占位</b>：写 null 会让"语义等价"的样式因为 null 占位差异
     * 产生不同的 Map 结构，作为池 key 时无法命中同一份 POI 对象。
     * <p>
     * 与写 null 占位相比，取值行为完全等价：{@link #getStyle(StyleKey)} 对"缺失"与"值为 null"
     * 同样返回 {@link Optional#empty()}；仅 {@link #containsKey} 会由 true 变为 false，
     * 而现有调用方（{@code CellStyles#createCellStyle}/{@code createFont}）在 containsKey 之后
     * 走的就是 getStyle，结果一致。
     *
     * @param keys 需要摘取的样式 key
     * @return 仅包含有值样式的样式字典（可能为空）
     */
    public StyleMap getStyleMap(Collection<StyleKey<?>> keys) {
        StyleMap style = new StyleMap();
        for (StyleKey<?> key : keys) {
            final String value = styleMap.get(key.getId());
            if (value != null) {
                style.styleMap.put(key.getId(), value);
            }
        }
        return style;
    }

    public void addStyle(StyleMap styleMap) {
        if (styleMap != null) {
            this.styleMap.putAll(styleMap.getStyleMap());
        }
    }

    public <T> T getStyle(StyleKey<T> key, T defaultValue) {
        final String value = styleMap.get(key.getId());
        if (value == null) {
            return defaultValue;
        }
        return key.getStyle(value);
    }

    public <T> Optional<T> getStyle(StyleKey<T> key) {
        if (containsKey(key)) {
            return Optional.ofNullable(getStyle(key, null));
        }
        return Optional.empty();
    }

    public boolean isEmpty() {
        return styleMap.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StyleMap) {
            return styleMap.equals(((StyleMap) obj).styleMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return styleMap.hashCode();
    }

    public boolean containsKey(StyleKey<?> key) {
        return styleMap.containsKey(key.getId());
    }

    public StyleMap createChildStyleMap(StyleMap childStyleMap) {
        StyleMap styleMap = new StyleMap();
        styleMap.addStyle(this);
        if (childStyleMap != null) {
            styleMap.addStyle(childStyleMap);
        }
        return styleMap;
    }
}
