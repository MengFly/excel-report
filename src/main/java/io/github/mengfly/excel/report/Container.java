package io.github.mengfly.excel.report;


import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleAble;

public interface Container extends StyleAble {

    /**
     * 获取容器的尺寸
     *
     * @return 容器尺寸
     */
    Size getSize();

    default void export(ReportContext context, Point point) {
        context.getStyleChain().onStyle(getStyle(), () -> onExport(context, point));
    }

    /**
     * 子组件必须实现该方法实现导出逻辑
     *
     * @param context 导出上下文
     * @param point   组件位置(左上角Cell位置)
     */
    void onExport(ReportContext context, Point point);

    /**
     * 打印组件信息
     * @return 组件信息
     */
    default String print() {
        return String.format("%s[%s]", getClass().getSimpleName(), getSize());
    }
}
