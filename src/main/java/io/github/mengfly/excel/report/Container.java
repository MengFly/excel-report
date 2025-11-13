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

    /**
     * 获取测量后的尺寸（组件实际尺寸）
     *
     * @return 测量后的尺寸
     */
    Size getMeasuredSize();

    /**
     * 获取组件位置
     *
     * @return 组件位置
     */
    Point getPosition();

    default void onMeasure() {
        onMeasure(getSize());
    }

    void onMeasure(Size suggestSize);

    default void onLayout() {
        onLayout(new Point(0, 0));
    }

    void onLayout(Point relativePosition);

    default void export(ReportContext context) {
        context.getStyleChain().onStyle(getStyle(), () -> onExport(context));
    }

    /**
     * 子组件必须实现该方法实现导出逻辑
     *
     * @param context 导出上下文
     */
    void onExport(ReportContext context);

    /**
     * 打印组件信息
     *
     * @return 组件信息
     */
    default String print() {
        return String.format("%s[%s]", getClass().getSimpleName(), getMeasuredSize());
    }


}
