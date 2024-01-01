package io.github.mengfly.excel.report.component.list;

import io.github.mengfly.excel.report.style.StyleHolder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ListHeader extends StyleHolder {
    private String title;
    private int span = 1;
}
