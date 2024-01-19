package io.github.mengfly.excel.report;

import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.layout.VLayout;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;

public class DrawUtil {

    public static void main(String[] args) {
        JFrame frame = new JFrame();

        ReportTemplate template = new ReportTemplate(DrawUtil.class.getClassLoader().getResourceAsStream("TestTemplate.xml"));

        frame.setSize(new Dimension(800, 600));

        final JScrollPane contentPane = new JScrollPane();
        final Container render = template.render(new DataContext());
        contentPane.getViewport().setView(new ContainerPointer(render));
        frame.setLocationRelativeTo(null);
        frame.setContentPane(contentPane);
        frame.setVisible(true);
    }


    @Getter
    @RequiredArgsConstructor
    public static class ContainerPointer extends JComponent {
        public static final int unitWidth = 72;
        public static final int unitHeight = 24;

        private final Container container;

        @Override
        public int getWidth() {
            return (container.getSize().width) * unitWidth + 5;
        }

        @Override
        public int getHeight() {
            return (container.getSize().height) * unitHeight + 5;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getWidth(), getHeight());
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            paint(g, container, 0, 0);
        }

        public void paint(Graphics g, Container container, int x, int y) {
            if (container instanceof HLayout) {
                int curX = x;
                for (Container child : ((HLayout) container).getContainers()) {
                    paint(g, child, curX, y);
                    curX += child.getSize().width * unitWidth;
                }
                g.drawRect(x, y, container.getSize().width * unitWidth, container.getSize().height * unitHeight);
            } else if (container instanceof VLayout) {
                int curY = y;
                for (Container child : ((VLayout) container).getContainers()) {
                    paint(g, child, x, curY);
                    curY += child.getSize().height * unitHeight;
                }
                g.drawRect(x, y, container.getSize().width * unitWidth, container.getSize().height * unitHeight);
            } else {
                g.drawRect(x, y, container.getSize().width * unitWidth, container.getSize().height * unitHeight);
                g.drawString(container.getClass().getSimpleName(), x, y + 20);
            }
        }
    }
}
