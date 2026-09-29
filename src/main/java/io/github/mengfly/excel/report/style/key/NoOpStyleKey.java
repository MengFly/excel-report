package io.github.mengfly.excel.report.style.key;

public class NoOpStyleKey<T> extends StyleKey<T> {

    public NoOpStyleKey(String id, Class<?> targetType, Class<T> styleType) {
        super(id, targetType, styleType, (target, style) -> {});
    }

}
