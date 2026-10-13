# Code Review 清单

> 最后更新：2026-10-13

## Bug 列表

| ID | 严重等级 | 文件 | 行号 | 描述 | 风险 | 建议方案 |
|----|---------|------|------|------|------|---------|
| 001 | 高 | `D:\job\b_project\b_backend\portal\src\main\resources\excel\WarningDayReport.xml` | 355, 360, 365 | 三个图片循环包裹层写 `<VLayout size="-1,-1" for="img: ...">`，高度 `-1` 是"弹性/按权重分剩余高度"而非内容自适应；父级按等权重均分得到 37/37/38 行，而三组内容实际为 32/48/32 行，3 条那组溢出 11 行压到下一个兄弟节点 | 导出直接失败：`IllegalStateException: Cannot add merged region A114:AE114 ... overlaps ... (A110:AE124)`；只要三组列表条数不等就必崩（2/3/2 崩、2/2/2 正常、4/3/3 崩） | 去掉包裹层的 `size="-1,-1"`，并把循环体内 `Text`/`Image` 宽度写成 `31`（整表 A..AE）。**不可只删 `size` 而不动子元素宽度**（见 003） |
| 002 | 高 | `src/main/java/io/github/mengfly/excel/report/layout/VLayout.java` | 65-76（配合 `util/WeightSizeHelper.java:65-83`） | 弹性子节点（`preferredSize.height < 0`）按 `weight`（默认 1）均分剩余高度；当分配值小于该节点的内容高度时，VLayout 既不裁剪也不告警，子元素继续顺序排布、溢出到下一个兄弟节点的行区间 | 两张合并区相交，POI 在 `addMergedRegion` 抛 `IllegalStateException`；报错发生在**导出阶段**且信息里只有单元格坐标、与模板位置无关，定位成本高（本次靠探针才定位到模板行） | 在 `VLayout.measure` 分配后校验：弹性子节点分配高度 < 其内容高度（`getSize().height`）时给出告警/日志（至少点名模板节点），不要静默溢出 |
| 003 | 中 | `src/main/java/io/github/mengfly/excel/report/layout/VLayout.java` | 49-63（`sumContainerSize`）、78-91（`getChildContainerSuggestSize`） | 省略 `size` 时 `preferredSize` 取 `container.getSize()`，而 `getSize().width = max(0, 子宽度)`，宽度为 `-1` 的子元素按 0 计入 ⇒ 包裹层自然宽度塌成 0，其子元素被测量成 0 宽 | 用户想表达"宽度自适应 + 高度随内容"时（`Size.of` 必须给两个值，写不出来）只能删 `size`，结果图片变成 0 宽；属静默错误，无任何提示 | `getSize()` 的宽度按"子元素全为 `-1` 则返回 `-1`"处理（或让 `sumContainerSize` 把 `-1` 透传），使省略 `size` 的包裹层宽度能继承父级可用宽度 |

## 详情

### 001 模板侧：弹性高度用在图片循环包裹层上

- 复现（框架真实导出链，探针 `target/probe/ExportProbe.java`）：
  - 原模板 + `WarningDayReport.json`（图片列表 2/3/2）→ 精确复现报错 `A114:AE114` vs `A110:AE124`
  - 原模板 + 三组均 2 条 → 导出成功（等分 32/32/32 恰好等于内容高度）
  - 原模板 + 4/3/3 → 崩（`A93:AE93` vs `A89:AE103`）
- 坐标对应：`A110:AE124` = 0-based 109–123，即"煤体应力图表"第 3 张图（溢出后）；`A114:AE114` = 0-based 113，即紧随其后的"支架压力图表"第 1 个标题。
- 每张图固定 15 行、标题 1 行 ⇒ 每组内容高度 = 条数 × 16。
- 修复后验证：2/3/2 与 4/3/3 均导出成功，合并区 242 个，行区间连续无重叠（105–119 / 121–135 / 137–151）。
- 备选方案（不推荐）：高度写死 `size="-1,32"/"-1,48"/"-1,32"` —— 条数一变即复发。

### 002 框架侧：弹性子节点超额分配后静默溢出

- 实测分配过程：整表 159 行，其余固定高度组件合计 47 行，剩余 112 行 ÷ 3 个等权重弹性子节点 = 37/37/38。
- 溢出路径：`VLayout.measure`（`suggestSize.height` 用分配值）→ 子元素均为固定高度 ⇒ `WeightSizeHelper` 对它们按声明值返回、无人吸收负的 `waitForDistributionSize` ⇒ `onLayout` 里 `start += childSize.height` 继续累加，越过容器自身范围。
- 同源风险：`HLayout` 的宽度分配走同一套 `WeightSizeHelper`，横向也会出现同类溢出（本次未实测）。

### 003 框架侧：省略 `size` 时宽度塌陷

- 实测：图片循环包裹层只删 `size="-1,-1"`（子元素仍为 `-1`）时，包裹层 `measured = (0, 32)`，其子元素 `measured = (0,1)`、`(0,15)` —— 宽度全为 0。
- 相关：`ContainerParser.doParse` 在 `size` 属性为空时用 `container.getSize()` 当 `preferredSize`，`SizeStyleKey` 把它写进样式；随后 `getChildContainerSuggestSize` 因 `preferredSize.width = 0 >= 0` 判定为"有确定宽度"而不回退到父级可用宽度。
