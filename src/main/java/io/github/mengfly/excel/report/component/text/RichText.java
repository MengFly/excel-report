package io.github.mengfly.excel.report.component.text;

import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.util.XmlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 富文本
 */
@Data
@Slf4j
public class RichText {

    private List<RichTextItem> items = new ArrayList<>();

    public RichText append(String text, StyleMap style) {
        items.add(new RichTextItem(text, style));
        return this;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public RichText append(String text) {
        items.add(new RichTextItem(text, null));
        return this;
    }

    public static boolean isRichText(String text) {

        return text.startsWith("<html>") && text.endsWith("</html>");
    }

    public static Object parse(String text) {
        try {
            final Document document = XmlUtil.parseXml(text);
            final Element rootElement = XmlUtil.getRootElement(document);
            RichText richText = new RichText();
            forEachElement(richText, rootElement, new StyleMap());
            return richText;
        } catch (Exception e) {
            log.error("parse rich text error", e);
            return text;
        }
    }

    private static void forEachElement(RichText richText, Element element, StyleMap styleMap) {
        final Map<String, String> attributeMap = XmlUtil.getAttributeMap(element);

        StyleMap fontStyleMap = styleMap.createChildStyleMap(CellStyles.createStyle(attributeMap));


        final NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node item = childNodes.item(i);
            if (item instanceof Element) {
                forEachElement(richText, (Element) item, fontStyleMap);
            } else if (item instanceof Text) {
                richText.append(item.getTextContent(), fontStyleMap);
            } else {
                System.out.println();
            }
        }


    }


    public RichTextString toRichTextString(ReportContext context, StyleMap superStyle) {

        if (superStyle == null) {
            superStyle = new StyleMap();
        }

        XSSFRichTextString rts = new XSSFRichTextString();
        for (RichTextItem item : items) {
            if (item.style != null) {
                final Font font = context.getFont(superStyle.createChildStyleMap(item.style));
                if (font == null) {
                    rts.append(item.getText());
                } else {
                    rts.append(item.getText(), ((XSSFFont) font));
                }
            } else {
                rts.append(item.getText());
            }
        }

        return rts;

    }

    @Data
    @AllArgsConstructor
    public static class RichTextItem {
        private String text;
        private StyleMap style;

        public String getText() {
            return text == null ? "" : text;
        }
    }

}
