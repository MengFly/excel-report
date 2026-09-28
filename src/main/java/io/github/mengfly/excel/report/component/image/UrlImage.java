package io.github.mengfly.excel.report.component.image;

import cn.hutool.core.img.ImgUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * 网络图片
 * <p>
 * 使用 {@link URLConnection} <b>同步</b>下载，并显式设置连接/读取超时。
 * <p>
 * ⚠️ 不能再用 hutool 的 {@code ImgUtil.getImage(URL)}：它内部是
 * {@code Toolkit.getDefaultToolkit().getImage(url)}，走 AWT <b>异步</b>加载，有两个致命问题：
 * <ol>
 *     <li>超时设不进去 —— 网络挂起时没有任何中断手段；</li>
 *     <li>失败不抛异常 —— 错误只进 {@code ImageObserver}，构造期的 catch 抓不到；
 *     而后续 {@code ImgUtil.toStream} → {@code toBufferedImage} → {@code copyImage} 会调
 *     {@code getWidth(null)} 强制同步等待，把当前线程<b>无限期阻塞</b>在下载上。</li>
 * </ol>
 * 改为同步下载后，超时与 HTTP 错误都在构造期抛出 IO 异常，由
 * {@code ImageComponent#onExport} 的 catch 降级为单元格文本（见该类 {@code setValue(e.getMessage())}）。
 */
@Slf4j
public class UrlImage implements Image {

    /**
     * 默认连接超时（毫秒）
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 5_000;
    /**
     * 默认读取超时（毫秒）
     */
    public static final int DEFAULT_READ_TIMEOUT = 10_000;

    private BufferedImage image;
    @Getter
    private String imageType;
    private Exception exception;

    /**
     * 使用默认超时（连接 5s / 读取 10s）
     *
     * @param url 图片地址
     */
    public UrlImage(String url) {
        this(url, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * @param url            图片地址
     * @param connectTimeout 连接超时（毫秒），0 表示不限制
     * @param readTimeout    读取超时（毫秒），0 表示不限制
     */
    public UrlImage(String url, int connectTimeout, int readTimeout) {
        try {
            final URLConnection connection = getUrlConnection(url, connectTimeout, readTimeout);
            try (InputStream stream = connection.getInputStream()) {
                image = ImageIO.read(stream);
            }
            if (image == null) {
                throw new IOException("Unsupported image content from " + url);
            }
            // 沿用原行为：统一按 JPEG 输出（PNG 的 alpha 会被丢弃）
            imageType = ImgUtil.IMAGE_TYPE_JPEG;
        } catch (Exception e) {
            exception = e;
            log.error("error load image data from : {}", url, e);
        }
    }

    @NonNull
    private static URLConnection getUrlConnection(String url, int connectTimeout, int readTimeout) throws IOException {
        final URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        if (connection instanceof HttpURLConnection) {
            final HttpURLConnection http = (HttpURLConnection) connection;
            // 图片地址常经 CDN / 短链跳转
            http.setInstanceFollowRedirects(true);
            final int code = http.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("Unexpected HTTP status " + code + " for " + url);
            }
        }
        return connection;
    }

    @Override
    public InputStream openStream() {
        if (image == null) {
            throw new RuntimeException(exception != null ? exception
                    : new IOException("image not loaded"));
        }
        return ImgUtil.toStream(image, ImgUtil.IMAGE_TYPE_JPEG);
    }
}
