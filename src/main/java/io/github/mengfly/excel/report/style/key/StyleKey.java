package io.github.mengfly.excel.report.style.key;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.style.StyleApplier;
import lombok.Getter;

import java.util.function.BiConsumer;

public class StyleKey<S> {
    @Getter
    private final String id;

    private final StyleApplier<?, S> styleApplier;

    public boolean isSupportApplyTarget(Object target) {
        if (target == null) {
            return false;
        }
        return isSupportApplyTarget(target.getClass());
    }

    public boolean isSupportApplyTarget(Class<?> targetType) {
        return targetType.isAssignableFrom(styleApplier.getTargetType())
                || styleApplier.getTargetType().isAssignableFrom(targetType);
    }

    public <T> StyleKey(String id, Class<T> targetType, Class<S> styleType, BiConsumer<T, S> applier) {
        this.id = id;
        this.styleApplier = new StyleApplier<T, S>(targetType, styleType) {
            @Override
            public void apply(T target, S style) {
                applier.accept(target, style);
            }
        };
    }


    public S getStyle(String property) {
        if (property == null) {
            return null;
        }
        if (styleApplier.getStyleType().isEnum()) {
            property = property.toUpperCase();
        }
        return Convert.convert(styleApplier.getStyleType(), property);
    }

    public String toString(S property) {
        return StrUtil.toString(property);
    }


    public void applyStyle(Object target, Object style) {
        styleApplier.applyStyle(target, style);
    }
}
