package io.github.mengfly.excel.report.style;

import java.util.Stack;

/**
 * 样式栈
 * <p>
 * 利用此类实现样式的层级继承
 */
public class StyleChain {

    private final Stack<StyleMap> styleChain = new Stack<>();
    private StyleMap currentStyle = new StyleMap();

    /**
     * 在该样式下进行操作
     * @param style 样式
     * @param styleConsumer 操作
     */
    public void onStyle(StyleMap style, Runnable styleConsumer) {
        if (style != null) {
            styleChain.push(style);
        }
        try {
            currentStyle = curStyle();
            styleConsumer.run();
        } finally {
            if (style != null) {
                styleChain.pop();
                currentStyle = curStyle();
            }
        }
    }

    private StyleMap curStyle() {
        StyleMap styleMap = new StyleMap();
        styleChain.forEach(styleMap::addStyle);
        return styleMap;
    }

    /**
     * 获取当前样式
     * @return 当前样式
     */
    public StyleMap getStyle() {
        return currentStyle;
    }

    /**
     * 创建当前样式的子样式
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
