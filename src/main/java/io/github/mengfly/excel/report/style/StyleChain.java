package io.github.mengfly.excel.report.style;

import java.util.Stack;

public class StyleChain {

    private final Stack<StyleMap> styleChain = new Stack<>();
    private StyleMap currentStyle = new StyleMap();

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

    public StyleMap getStyle() {
        return currentStyle;
    }

    public StyleMap getStyle(StyleMap styleMap) {
        if (styleMap == null || styleMap.isEmpty()) {
            return getStyle();
        }
        return getStyle().createChildStyleMap(styleMap);
    }

}
