package io.github.mengfly.excel.report.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtils extends org.springframework.util.StringUtils {

    public static boolean isNotEmpty(String str) {
        return hasLength(str);
    }

    public static boolean isEmptyOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String trim(String substring) {
        if (substring == null) {
            return null;
        }
        return substring.trim();
    }

    public static List<String> splitTrim(String args, char c) {
        if (args == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(args.split(String.valueOf(c)))
                .filter(s -> !isEmptyOrBlank(s))
                .map(StringUtils::trim)
                .collect(Collectors.toList());

    }

    public static boolean equalsAnyIgnoreCase(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        }
        return s1.equalsIgnoreCase(s2);
    }
}
