package io.github.mengfly.excel.report.component.table;

import io.github.mengfly.excel.report.util.BeanUtil;
import lombok.extern.slf4j.Slf4j;

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
        return BeanUtil.getBeanObj(object, getId());
    }


}
