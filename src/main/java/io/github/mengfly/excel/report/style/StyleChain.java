package io.github.mengfly.excel.report.style;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 样式栈
 * <p>
 * 利用此类实现样式的层级继承
 */
public class StyleChain {

    /**
     * 层级堆叠样式表：每一层保存合并后的样式表
     */
    private final Deque<StyleMap> mergedStack = new ArrayDeque<>();

    /**
     * 在该样式下进行操作
     *
     * @param style         样式
     * @param styleConsumer 操作
     */
    public void onStyle(StyleMap style, Runnable styleConsumer) {
        final int depth = mergedStack.size();
        if (style != null) {
            // 获取父 merged 样式（不移除），合并子样式
            mergedStack.push(mergeInto(mergedStack.peek(), style));
        }
        try {
            styleConsumer.run();
        } finally {
            while (mergedStack.size() > depth) {
                mergedStack.pop();
            }
        }
    }

    private StyleMap mergeInto(StyleMap parent, StyleMap child) {
        StyleMap merged = new StyleMap();
        merged.addStyle(parent);
        merged.addStyle(child);
        return merged;
    }

    /**
     * 获取当前样式
     *
     * @return 当前样式
     */
    public StyleMap getStyle() {
        final StyleMap currentStyle = mergedStack.peek();
        return currentStyle == null ? new StyleMap() : currentStyle;
    }

    /**
     * 创建当前样式的子样式
     *
     * @param styleMap 子样式
     * @return 当前样式的子样式
     */
    public StyleMap getStyle(StyleMap styleMap) {
        if (styleMap == null || styleMap.isEmpty()) {
            return getStyle();
        }
        return getStyle().createChildStyleMap(styleMap);
    }

}
