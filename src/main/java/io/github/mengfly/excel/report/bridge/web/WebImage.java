package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

/**
 * 图片单元格的内容与渲染参数。
 *
 * @author Mengfly
 */
@Getter
@Setter
public class WebImage extends WebCellContent {

    /**
     * 图片的 base64 dataURL；读取失败时为 null。
     */
    private String dataUrl;

    /**
     * 缩放类型（ScaleType 枚举名小写，如 fit_xy / fit_start / fit_end / center）。
     */
    private String scaleType;

    /**
     * 高度缩放比例。
     */
    private Double scaleHeight;

    /**
     * 内边距，形如 {@code "top,right,bottom,left"}（单位：像素）。
     */
    private String padding;

    /**
     * 锚点类型（ClientAnchor.AnchorType 枚举名）。
     */
    private String anchorType;

    public WebImage() {
        setKind("image");
    }
}