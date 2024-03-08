package io.github.mengfly.excel.report.component.chart.data;

import lombok.Data;
import org.apache.poi.xddf.usermodel.text.*;

@Data
public class ChartTitle {

    private boolean overLay = false;
    private String text;
    private boolean bold = true;
    private double fontSize = 12;
    private UnderlineType underLine = UnderlineType.NONE;
    private Integer baseLine;
    private boolean italic = false;

    // TODO 添加字体填充支持

    /**
     * 字体间距
     */
    private Double characterSpacing;
    /**
     * 删除线
     */
    private StrikeType strikeThrough = StrikeType.NO_STRIKE;

    public void initTextBodyStyle(XDDFTextBody body) {
        for (XDDFTextParagraph paragraph : body.getParagraphs()) {
            final XDDFRunProperties properties = paragraph.getDefaultRunProperties();
            paragraph.setText(text);

            properties.setBold(bold);
            properties.setFontSize(fontSize);
            properties.setUnderline(underLine);

            if (baseLine != null) {
                properties.setBaseline(baseLine);
            }
            properties.setItalic(italic);
            if (characterSpacing != null) {
                properties.setCharacterSpacing(characterSpacing);
            }
            properties.setStrikeThrough(strikeThrough);
        }
    }
}
