package io.github.mengfly.excel.report.component.image;

import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Base64Image implements Image {
    public static final String BASE64_HEADER = "data:image/";

    private final String src;
    @Getter
    private final String imageType;

    public Base64Image(String src) {
        final int indexOf = src.indexOf(',');

        imageType = src.substring(0, indexOf).split(";")[0].substring(BASE64_HEADER.length());
        this.src = src.substring(indexOf + 1);
    }

    public static boolean isBase64Image(String src) {
        return src.startsWith(BASE64_HEADER);
    }


    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(src.getBytes());
    }
}
