package io.github.mengfly.excel.report;


import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleAble;
import io.github.mengfly.excel.report.template.ContainerTreeNode;

public interface Container extends StyleAble {

    /**
     * 获取该组件相关的模板信息
     * <br>
     * 该方法只有在通过模板方法创建的时候才会返回信息，否则返回null
     *
     * @return 模板信息
     */
    ContainerTreeNode getTemplateNode();

    void setTemplateNode(ContainerTreeNode templateNode);

    /**
     * 获取容器的尺寸（组件基础尺寸）
     *
     * @return 容器尺寸
     */
    Size getSize();

    default void export(ReportContext context, Point point, Size suggestSize) {
        context.getStyleChain().onStyle(getStyle(), () -> onExport(context, point, suggestSize));
    }

    /**
     * 子组件必须实现该方法实现导出逻辑
     *
     * @param context 导出上下文
     * @param point   组件位置(左上角Cell位置)
     */
    void onExport(ReportContext context, Point point, Size suggestSize);

    /**
     * 打印组件信息
     *
     * @return 组件信息
     */
    default String print() {
        return String.format("%s[%s]", getClass().getSimpleName(), getSize());
    }


}
