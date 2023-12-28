package io.github.mengfly.excel.report.template.parse;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import lombok.Getter;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.image.*;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

import java.io.File;
import java.io.InputStream;

@Getter
public class ImageParser extends ContainerParser {
    private final String tagName = "Image";

    @Override
    public Container parse(ContainerTreeNode containerTreeNode, DataContext context) {
        final ImageComponent component = new ImageComponent();
        component.setSize(getSize(containerTreeNode, context, Size.of(4, 10)));

        String attribute = containerTreeNode.getAttribute("src");
        final Object imageSource = context.doExpression(attribute);
        component.setImage(deduceImage(imageSource));

        return component;
    }

    private Image deduceImage(Object imageSource) {
        if (imageSource == null) {
            return null;
        }
        if (imageSource instanceof Image) {
            return ((Image) imageSource);
        }
        if (imageSource instanceof InputStream) {
            return new StreamImage((InputStream) imageSource);
        }
        if (imageSource instanceof File) {
            return new FileSystemImage((File) imageSource);
        }
        if (imageSource instanceof String) {
            String src = (String) imageSource;
            if (Base64Image.isBase64Image(src)) {
                return new Base64Image(src);
            }
            if (FileUtil.isFile(((String) imageSource))) {
                return new FileSystemImage(new File((String) imageSource));
            }
            if (HttpUtil.isHttp(src) || HttpUtil.isHttps(src)) {
                return new UrlImage(src);
            }
        }
        return null;
    }


}
