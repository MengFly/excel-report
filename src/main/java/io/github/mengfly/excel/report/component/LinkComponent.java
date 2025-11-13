package io.github.mengfly.excel.report.component;

import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.CellStyles;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.Hyperlink;

@Getter
@Setter
public class LinkComponent extends TextComponent {

    private String link;
    private String label;

    {
        addStyle(CellStyles.fontUnderline, FontUnderline.SINGLE);
        addStyle(CellStyles.fontColor, CellStyles.createColor(0x2570d4));
    }

    @Override
    public void onExport(ReportContext context) {
        ExcelCellSpan cellSpan = context.getCellSpan(getPosition(), getMeasuredSize());
        cellSpan.merge().setValue(getText());

        final Hyperlink hyperlink = context.createUrlHyperlink(link, label);
        cellSpan.setHyperLink(hyperlink);
    }
}
