package io.github.mengfly.excel.report.component.table;

import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class TableObjFieldColumn extends TableColumn {

    public static TableObjFieldColumn of(String id, String name) {
        return of(id, name, 1);
    }

    public static TableObjFieldColumn of(String id, String name, int columnSpan) {
        final TableObjFieldColumn fieldColumn = new TableObjFieldColumn();
        fieldColumn.setId(id);
        fieldColumn.setName(name);
        fieldColumn.setColumnSpan(columnSpan);
        return fieldColumn;
    }

    @Override
    public Object getValue(Object object) {
        if (object == null) {
            return null;
        }
        try {
            if (object instanceof Map) {
                return ((Map<?, ?>) object).get(getId());
            }
            return ReflectUtil.getFieldValue(object, getId());
        } catch (Exception e) {
            log.error("Error getting column value on column {}", this, e);
        }
        return null;
    }
}
