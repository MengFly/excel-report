package io.github.mengfly.excel.report.util;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyDescriptor;
import java.util.*;

@Slf4j
public class BeanUtil extends cn.hutool.core.bean.BeanUtil {


    public static void initBeanProperties(Object bean, Map<String, ?> attributeMap) {
        if (bean == null) {
            return;
        }


        final Class<?> beanClass = bean.getClass();
        attributeMap.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            PropertyDescriptor propertyDescriptor = getPropertyDescriptor(beanClass, key);

            if (propertyDescriptor == null) {
                return;
            }

            settingBeanProperties(bean, propertyDescriptor, value);

        });
    }

    private static void settingBeanProperties(Object bean, PropertyDescriptor propertyDescriptor, Object settingValue) {
        try {
            if (!propertyDescriptor.getPropertyType().isInstance(settingValue)) {
                if (propertyDescriptor.getPropertyType().isEnum()) {
                    final Map<String, Enum<?>> enumMap = getEnumMap(propertyDescriptor.getPropertyType());
                    String enumKey = String.valueOf(settingValue).replaceAll(" ", "").replaceAll("_", "").toLowerCase();

                    settingValue = enumMap.get(enumKey);
                }

                settingValue = Convert.convert(propertyDescriptor.getPropertyType(), settingValue);
            }
            if (settingValue == null) {
                return;
            }
            if (propertyDescriptor.getWriteMethod() != null) {
                propertyDescriptor.getWriteMethod().invoke(bean, settingValue);
            } else {
                ReflectUtil.setFieldValue(bean, propertyDescriptor.getName(), settingValue);
            }
        } catch (Exception ignore) {

        }

    }

    private static Map<String, Enum<?>> getEnumMap(Class<?> clazz) {

        final Object[] enumConstants = clazz.getEnumConstants();

        Map<String, Enum<?>> resultMap = new HashMap<>();

        if (enumConstants == null) {
            return resultMap;
        }
        for (Object enumConstant : enumConstants) {
            String identifier = ((Enum<?>) enumConstant).name().replaceAll("_", "").toLowerCase();

            resultMap.put(identifier, (Enum<?>) enumConstant);
        }
        return resultMap;
    }

    public static Object getBeanObj(Object bean, String attr) {
        if (StrUtil.isEmpty(attr)) {
            return bean;
        }
        final String[] split = attr.split("\\.");

        for (String field : split) {
            bean = getByField(bean, field);
        }
        return bean;
    }

    private static Object getByField(Object bean, String field) {
        if (bean == null) {
            return null;
        }
        try {
            if (bean instanceof Map) {
                return ((Map<?, ?>) bean).get(field);
            }
            return ReflectUtil.getFieldValue(bean, field);
        } catch (Exception e) {
            log.error("Error getting bean value {} for {}", field, bean.getClass().getSimpleName(), e);
        }
        return null;
    }

    public static boolean isList(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Iterable) {
            return true;
        }
        if (obj instanceof Iterator) {
            return true;
        }
        return ArrayUtil.isArray(obj);
    }

    public static List<?> objectToList(Object dataList) {

        if (dataList == null) {
            return Collections.emptyList();
        }
        if (dataList instanceof List) {
            return ((List<?>) dataList);
        }
        if (dataList instanceof Iterable) {
            return ListUtil.toList(((Iterable<?>) dataList).iterator());
        }
        if (dataList instanceof Iterator) {
            return ListUtil.toList(((Iterator<?>) dataList));
        }
        if (ArrayUtil.isArray(dataList)) {
            final Object[] cast = ArrayUtil.cast(ArrayUtil.getComponentType(dataList), dataList);
            return ListUtil.toList(cast);
        }
        log.error("该类型数据不是列表数据 : {}", dataList.getClass());
        return Collections.emptyList();
    }
}
