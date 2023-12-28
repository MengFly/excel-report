package io.github.mengfly.excel.report.component.image;

import cn.hutool.core.io.FileUtil;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Data
public class FileSystemImage implements Image {

    private final File file;

    @Override
    public String getImageType() {
        return FileUtil.getSuffix(file);
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(file.toPath());
    }
}
