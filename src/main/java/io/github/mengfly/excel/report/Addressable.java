package io.github.mengfly.excel.report;

import org.apache.poi.ss.util.CellRangeAddress;

public interface Addressable {
    
    CellRangeAddress getAddress();
}
