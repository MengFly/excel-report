package io.github.mengfly.excel.report.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point {

    private int x;
    private int y;

    public Point add(Point point) {
        return add(point.x, point.y);
    }

    public Point add(int x, int y) {
        return new Point(this.x + x, this.y + y);
    }
}
