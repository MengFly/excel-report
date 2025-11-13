package io.github.mengfly.excel.report.util;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StyleUtil {

    /**
     * 分析样式
     *
     * @param style 样式
     * @return 样式
     */
    public static List<String> analysisStyle(String style) {
        if (StrUtil.isEmpty(style)) {
            return Collections.emptyList();
        }
        List<String> styleTokens = new ArrayList<>();

        int tokenDepth = 0;

        StringBuilder token = new StringBuilder();
        for (int i = 0; i < style.length(); i++) {
            char c = style.charAt(i);

            if (c == ' ') {
                // 到结尾位置了
                if (tokenDepth > 0) {
                    token.append(c);
                } else {
                    addToken2List(styleTokens, token);
                    tokenDepth = 0;
                }
            } else if (c == '{') {
                tokenDepth++;
                token.append(c);
            } else if (c == '}') {
                tokenDepth--;
                token.append(c);
            } else {
                token.append(c);
            }
        }
        addToken2List(styleTokens, token);
        return styleTokens;
    }

    private static void addToken2List(List<String> styleTokens, StringBuilder token) {
        final String str = token.toString();
        if (StrUtil.isEmpty(str) || StrUtil.isBlank(str)) {
            return;
        }
        styleTokens.add(str.trim());

        token.delete(0, token.length());
    }
}
