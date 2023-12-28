package io.github.mengfly.excel.report.component.image;


import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.InputStream;

public interface Image {

    String getImageType();

    InputStream openStream() throws IOException;


    default int deduceExcelImageType() {
        String imageType = getImageType();
        if (imageType == null) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        imageType = imageType.toLowerCase();
        switch (imageType) {
            case "jpg":
            case "jpeg":
                return Workbook.PICTURE_TYPE_JPEG;
            case "png":
                return Workbook.PICTURE_TYPE_PNG;
            case "emf":
                return Workbook.PICTURE_TYPE_EMF;
            default:
                throw new IllegalArgumentException("Unknown image type: " + imageType);
        }
    }
}
