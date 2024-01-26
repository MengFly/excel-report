package io.github.mengfly.excel.report.component.text;

import cn.hutool.http.HtmlUtil;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.util.XmlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.w3c.dom.Document;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.DocumentParser;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 富文本
 */
@Data
public class RichText {

    private List<RichTextItem> items = new ArrayList<>();

    public RichText append(String text, StyleMap style) {
        items.add(new RichTextItem(text, style));
        return this;
    }

    public RichText append(String text) {
        items.add(new RichTextItem(text, null));
        return this;
    }

    public static boolean isRichText(String text) {

        return text.startsWith("<html>") && text.endsWith("</html>");
    }

    public static RichText parse(String text) {
        //final Document document = XmlUtil.parseXml(text);
        //DocumentParser parser = new DocumentParser(DTD.getDTD("html32"));
        //RichText richText = new RichText();
        //
        //try {
        //
        //
        //    parser.parse(new CharArrayReader(text.toCharArray()), new HTMLEditorKit.ParserCallback() {
        //
        //
        //
        //    }, true);
        //} catch (IOException e) {
        //    return richText.append(HtmlUtil.cleanHtmlTag(text));
        //}
        return new RichText();
    }



    @Data
    @AllArgsConstructor
    public static class RichTextItem {
        private String text;
        private StyleMap style;
    }

}
