package io.github.mengfly.excel.report.style.key;

public class NoOpStyleKey<T> extends StyleKey<T> {


    public NoOpStyleKey(String id, Class<T> type) {
        super(id, null, type);
    }

    @Override
    public void applyStyle(Object target, Object style) {
        // do nothing
    }
}
