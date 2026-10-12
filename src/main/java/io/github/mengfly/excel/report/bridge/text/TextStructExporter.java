package io.github.mengfly.excel.report.bridge.text;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.bridge.BridgeExporter;
import io.github.mengfly.excel.report.excel.ExportResult;
import io.github.mengfly.excel.report.layout.Layout;

public final class TextStructExporter implements BridgeExporter<String> {

    public static final TextStructExporter instance = new TextStructExporter();

    @Override
    public String export(ExportResult export) {
        if (export == null || export.getContainer() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        collect(export.getContainer(), sb);
        return sb.toString();
    }

    private void collect(Container container, StringBuilder sb) {
        final String simpleName = container.getClass().getSimpleName().replace("Component", "");
        sb.append(String.format("%s[%s]", simpleName, container.getMeasuredSize()));
        if (container instanceof Layout) {

            sb.append(" {\n");
            for (Container c : ((Layout) container).getContainers()) {
                StringBuilder child = new StringBuilder();
                collect(c, child);
                final String[] split = child.toString().split("\n");
                for (String s : split) {
                    sb.append("    ").append(s).append("\n");
                }
            }
            sb.append("}");
        }

    }
}
