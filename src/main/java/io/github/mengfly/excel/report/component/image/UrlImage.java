package io.github.mengfly.excel.report.component.image;

import cn.hutool.core.img.ImgUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;

@Slf4j
public class UrlImage implements Image {

    private java.awt.Image image;
    @Getter
    private String imageType;
    private Exception exception;

    public UrlImage(String url) {
        try {
            image = ImgUtil.getImage(new URL(url));
            imageType = ImgUtil.IMAGE_TYPE_JPEG;
        } catch (Exception e) {
            exception = e;
            log.error("error load image data from : {}", url, e);
        }
    }

    @Override
    public InputStream openStream() {
        if (image == null) {
            throw new RuntimeException(exception);
        }
        return ImgUtil.toStream(image, ImgUtil.IMAGE_TYPE_JPEG);
    }
}
