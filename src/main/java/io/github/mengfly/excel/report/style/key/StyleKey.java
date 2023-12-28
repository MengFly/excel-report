package io.github.mengfly.excel.report.style.key;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
@RequiredArgsConstructor
public class StyleKey<T> {
    @Getter
    private final String id;
    private final String methodName;
    private final Class<T> type;


    public T getStyle(String property) {
        if (property == null) {
            return null;
        }
        if (type.isEnum()) {
            property = property.toUpperCase();
        }
        return Convert.convert(type, property);
    }

    public String toString(T property) {
        return StrUtil.toString(property);
    }


    public void applyStyle(Object target, Object style) {
        if (target == null) {
            return;
        }
        try {
            Method method = ReflectUtil.getMethod(target.getClass(), methodName, type);
            ReflectUtil.invoke(target, method, style);
        } catch (Exception e) {
            log.error("Can't set style property {} for cell value [{}]{}",
                    getId(), style.getClass().getSimpleName(), style);
        }
    }

}
