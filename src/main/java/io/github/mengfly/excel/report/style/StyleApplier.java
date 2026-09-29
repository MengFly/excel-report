package io.github.mengfly.excel.report.style;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class StyleApplier<T, S> {

    private final Class<T> targetType;
    private final Class<S> styleType;


    public void applyStyle(Object target, Object style) {
        if (target == null || style == null) {
            return;
        }

        if (targetType.isInstance(target) && styleType.isInstance(style)) {
            //noinspection unchecked
            apply((T) target, (S) style);
        }


    }

    public abstract void apply(T target, S style);
}
