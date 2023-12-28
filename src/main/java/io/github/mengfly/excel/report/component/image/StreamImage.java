package io.github.mengfly.excel.report.component.image;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.IoUtil;

import java.io.InputStream;

public class StreamImage implements Image {

    private final java.awt.Image image;


    public StreamImage(InputStream stream) {
        this.image = ImgUtil.toImage(IoUtil.readBytes(stream, true));
    }

    @Override
    public String getImageType() {
        return ImgUtil.IMAGE_TYPE_JPEG;
    }

    @Override
    public InputStream openStream() {
        return ImgUtil.toStream(image, ImgUtil.IMAGE_TYPE_JPEG);
    }
}
