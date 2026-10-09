# JSON/HTML 渲染 Starter 开发指南（路径 X：零侵入）

> 面向：`excel-report-json-spring-boot-starter`（独立仓库，不在本框架内开发）
> 依赖框架：`io.github.mengfly:excel-report:1.5.1+`（Central 发布版，不依赖本地未提交源码）
> 定位：**路径 X = 零侵入**。Starter 只调用框架公开 API，把 Excel 导出链路当"布局引擎"跑一遍，
> 再从内存 workbook 读回终态几何，序列化成 JSON；前端用纯 CSS + ECharts 渲染。
> 关联讨论：本方案的扩展性背景见 `优化建议.md` 四.5 残余可做项（第二后端出现时才考虑 RenderContext 抽象，即路径 Y）。

---

## 1. 目标与保真度口径

| 目标 | 说明 |
|---|---|
| 每列宽高与 Excel 完全一致 | 列宽/行高**直接从导出后的 workbook 读回**（同一来源，天然 100%） |
| 图片位置尺寸一致 | anchor 已是终态（scaleType/padding 修正发生在导出 finalizer 中），读回即得 |
| 图表数据/类型还原 | export 后图表数据（无论来自 SpEL 还是单元格引用）都已写入 chart XML 缓存，读回解析 |
| 无第三方前端框架 | 前端只有 vanilla JS + ECharts（用户豁免项） |

**保真度的诚实定义**：网格几何、合并区、图片矩形、图表位置尺寸 **100%**（与 Excel 同源）；
行内文本排版（换行位置、字形渲染）**≈95%**（浏览器字体引擎与 Excel 不同，无法消除）。

**不做的事**：打印设置（页面边距/方向）、`PageRowSplit/PageColSplit` 的打印分页符（可选用 CSS break 标记，见 §5.4）、SXSSF/大批量场景（框架定位本就不含，见台账三.6）。

---

## 2. 总体架构

```
模板 XML ─┐
数据 JSON ─┤→ ReportTemplate.render(dataContext)         （框架：表达式求值、if/for、组件树构建）
           │      │
           │      ├─ ExcelReport.exportTemplate(...)      （框架：measure → layout → export →
           │      │        │                               auto 宽高补算 → finalizer 执行完毕）
           │      │        │
           │      │   XSSFWorkbook（内存，不 save 不落盘）
           │      │        │
           │      │   Starter 读回（只走 POI 公开 API）：
           │      │        ├─ 列宽 px / 行高 px / 合并区
           │      │        ├─ cell 值（DataFormatter）+ 样式 + 超链接 + 富文本 run
           │      │        ├─ 图片：drawing anchor（终态 EMU）+ 图片字节 → base64
           │      │        └─ 图表：chart XML（CTChartSpace）→ ECharts option
           │      │
           │      └─ JSON 报文 ──→ 前端渲染器（绝对定位 div + ECharts）
           │
           └─ （同一链路照常 save() 出 xlsx，两通道产物共享同一次导出）
```

**关键事实（决定了为什么可行）**：
1. `exportTemplate` 返回时，`applyCellWidthHeight` 已执行：auto 列宽补算、行高逐行写入、图片/图表 finalizer 全部完成。**不需要 save()，内存对象即为终态**。
2. 所有组件（Text/Table/List/Span/Link）最终都落在"cell + 样式 + 合并区"里，读回时自动坍缩，**Starter 不需要理解组件树**。
3. `ChartValueAxisData` 的数据两种来源（`ValueAxisDataResolver` 字面量 / `RelationAxisDataResolver` 单元格区域）在 export 后都落进 chart XML 的 `strCache/numCache` ⇒ 统一从 chart XML 读，无需碰组件对象。

---

## 3. 允许依赖的 API 面（白名单）

### 3.1 框架 API（全部 public）

| 调用 | 用途 |
|---|---|
| `new ExcelReport()` | 每次渲染新建一个（workbook 绑定实例，禁止跨线程复用） |
| `ExcelReport#exportTemplate(ReportTemplate, String name, DataContext)` | 渲染+导出一个 sheet，返回根 `Container`（Starter 可忽略返回值） |
| `ExcelReport#exportSheet(String, Container, StyleMap)` | 程序化构建的组件树（可选支持） |
| `ExcelReport#getWorkbook()` | 读回入口（Lombok `@Getter`，public，属稳定契约） |
| `TemplateManager#getTemplate(name)` / `setStrict(boolean)` | 模板缓存（classpath/dir 工厂） |
| `new ReportTemplate(InputStream)` | 直接从字节解析（自带两层校验 `validate()`） |
| `new DataContext(Map)` | 数据上下文（SpEL 变量来源） |
| `ReportTemplate#getSheetStyle()` | 透传给 exportTemplate |

### 3.2 POI API（读回用，5.2.x）

全部走 `XSSF*` 公开类型：`XSSFSheet` / `XSSFCellStyle` / `XSSFFont` / `XSSFColor` / `XSSFDrawing` / `XSSFPicture` / `XSSFChart` / `XSSFRichTextString`（含 `getCTRst()`）。
**禁止** import 框架的 `io.github.mengfly.excel.report.excel.*`（`ReportContext`/`ExcelCellSpan` 属内部实现；将来路径 Y 重构它们时 Starter 零感知）。

---

## 4. 服务端实现规格

### 4.1 渲染主流程（骨架，JDK 8 语法）

```java
public RenderedReport renderJson(String templateXml, Map<String, Object> data) {
    ReportTemplate template = new ReportTemplate(
            new ByteArrayInputStream(templateXml.getBytes(StandardCharsets.UTF_8)));
    // 可选：template.validate() 非空时打 WARN（宽松模式不中断）

    ExcelReport report = new ExcelReport();
    report.exportTemplate(template, null, new DataContext(data));  // name=null → "sheet"

    return readBack(report.getWorkbook());
}
```

### 4.2 网格几何

```java
XSSFSheet sheet = workbook.getSheetAt(0);

// 列：0..maxCol-1；maxCol = 所有行最后一个有 cell 的列号 + 1
//   （Text/Image/Chart/Span 的 ExcelCellSpan 构造器会把覆盖区域的 cell 全建出来，
//    所以"有 cell"即覆盖过的区域，anchor 不会越界于 cell 网格）
double[] cols = new double[maxCol];
for (int c = 0; c < maxCol; c++) cols[c] = sheet.getColumnWidthInPixels(c);   // 直接就是 px @96dpi

// 行：0..lastRowNum；px = points × 4/3
double[] rows = new double[sheet.getLastRowNum() + 1];
for (int r = 0; r < rows.length; r++) {
    Row row = sheet.getRow(r);
    rows[r] = (row == null ? sheet.getDefaultRowHeightInPoints() : row.getHeightInPoints()) * 4 / 3;
}
```

- 行高单位换算：`points × 96 / 72 = × 4/3`。框架默认行高 20pt ⇒ 26.67px。
- **不要自己换算列宽**：`getColumnWidthInPixels` 已按默认字体 MDW 计算（Calibri 11 下 ≈7px/字符单位），直接信任返回值。
- rect 坐标 = 前缀和：`x = Σ cols[0..x-1]`。列宽是小数（如 62.375px），**JSON 保留浮点、前端不取整**，累加才有亚像素精度。

### 4.3 单元格与样式

遍历 rows × cells：

```java
DataFormatter formatter = new DataFormatter();
for (Row row : sheet) for (Cell cell : row) {
    // 1. 合并区归属：只输出"区域左上角"的 cell；其余格子跳过（渲染端由 w/h 覆盖）
    //    sheet.getMergedRegions() 预建 Map<firstRow*1_000_000+firstCol, CellRangeAddress>
    // 2. 文本：String s = formatter.formatCellValue(cell);   ← 数字/日期按 dataFormat 显示，与 Excel 所见一致
    //    富文本判定：cell instanceof ... RichTextString with 多 run，见 4.4
    // 3. 超链接：Hyperlink link = cell.getHyperlink();  → link.getAddress()
    // 4. 样式：见映射表
}
```

**样式映射表（`XSSFCellStyle` → JSON css 字段）**：

| Excel 读取 | JSON css | 说明 |
|---|---|---|
| `getFont()` → `XSSFFont.getFontName()` | `fontFamily` | 前端需有对应 web font，否则字形漂移 |
| `getFontHeightInPoints()` | `fontSize` (pt→px: ×4/3) | 别用 `getFontHeight()`（1/20pt 单位，20pt→400） |
| `getBold()/getItalic()/getStrikeout()` | `fontWeight:700` / `fontStyle:italic` / `text-decoration:line-through` | |
| `getUnderline()` (short) | `text-decoration:underline` | 0=无，1=SINGLE，2=DOUBLE（`FontUnderline.valueOf(short)`） |
| `getXSSFColor().getRGB()` | `color: #rrggbb` | **XSSFColor 没有 getRGBHexString**（5.2.5 实测），用 `HexUtil.encodeHexStr(getRGB())`；返回 null = 主题色/索引色，回退继承 |
| `getAlignmentEnum()` | `justifyContent`（flex） | LEFT/CENTER/RIGHT/JUSTIFY |
| `getVerticalAlignmentEnum()` | `alignItems`（flex） | TOP/CENTER/BOTTOM |
| `getWrapText()` | `white-space:normal; overflow-wrap:anywhere` | false → `nowrap` |
| `getRotation()` (short) | 见 §6 rotation 规则 | 0–180 逆时针；255=竖排 |
| `getIndention()` (short) | `padding-left ≈ indent × 9px` | Excel 每级缩进≈3 空格宽，近似值 |
| `getFillPatternEnum()==SOLID` | `background: #fg` | **坑**：SOLID 填充的可见色是 **foregroundColor**（`getFillForegroundColorColor()`），不是 backgroundColor |
| `getBorderTopEnum()` + `getTopBorderXSSFColor()` | `border-top` | 四边同理；POI 的 `getXxxBorderColor()` 返回 short 索引没用，**必须用 `getXxxBorderXSSFColor()`** |
| `getDataFormatString()` | 不映射（文本已格式化） | 如需前端重排/排序，可附带 `value` 原始值 |

**样式省略规则**：css 对象只包含**非默认值**（无样式 cell 输出 `"css":{}` 或省略），控制 JSON 体积。

**边框叠加说明**：`ExcelCellSpan.setStyle` 会把左上角样式刷满整个合并区，所以区域内每个格子的边框样式相同 ⇒ 渲染端"每格画自己的边框"即可复现 Excel 外观，无需做 border-collapse；相邻非合并格子的共享边会出现双边框（1px 重叠），视觉可接受，追求精确可在渲染端按"相邻同边取粗者"去重（P2）。

### 4.4 富文本（RichText）

```java
if (cell.getCellType() == STRING) {
    XSSFRichTextString rich = (XSSFRichTextString) cell.getRichStringCellValue();
    if (rich.numFormattingRuns() > 1 || rich.getCTRst().getRArray().length > 0) {
        // 遍历 CTRst 的 r 数组：每个 CTRElt → getT()（片段文本）+ getRPr()（rFont/sz/b/i/u/color）
        // 无 rPr 的片段继承 cell 级字体
    }
}
```

框架的 `${...}` 富文本标记在 `setValue` 时已解析成 XSSFRichTextString，读回即得最终 run 序列。

### 4.5 图片

```java
XSSFDrawing drawing = sheet.getDrawingPatriarch();   // 无图形时返回 null，注意判空
for (XSSFPicture pic : drawing.getPictures()) {
    XSSFClientAnchor a = pic.getAnchor();            // 终态 anchor（finalizer 已改完）
    // rect.x = prefixSumCols(a.getCol1()) + a.getDx1()/9525.0
    // rect.y = prefixSumRows(a.getRow1()) + a.getDy1()/9525.0
    // rect.w = (prefixSumCols(a.getCol2()) + a.getDx2()/9525.0) - rect.x
    // rect.h = (prefixSumRows(a.getRow2()) + a.getDy2()/9525.0) - rect.y
    // data  = pic.getPictureData().getData();  mime = "image/" + pic.getPictureData().suggestFileExtension()
}
```

- EMU→px：`/ 9525`（96dpi）；EMU→pt：`/ 12700`。
- `ScaleType`/`padding`/`scaleHeight` 的修正**全部已烘焙进 anchor**（导出 finalizer），读回就是最终矩形，**Starter 不需要复现任何缩放逻辑**。
- 防御：若出现 `oneCellAnchor`（本框架当前只用 twoCellAnchor，但别崩），用 `a.getDx2()` 前先判 `CTTwoCellAnchor` 是否有 ext，没有则按图片原始尺寸×EMU 换算。

### 4.6 图表 → ECharts option

**统一从 chart XML 读**（`drawing.getCharts()` 拿 `XSSFChart`，`getCTChartSpace()` 拿全量 XML；图表矩形从所在 `CTTwoCellAnchor` 的 from/to 算，同 §4.5 换算）。

解析路径（XmlBeans CT 对象，公开 API）：

```
chartSpace > chart > plotArea
  ├ lineChart / barChart / areaChart / scatterChart / pieChart / bar3DChart   ← 图表组，每组若干 ser
  │   ser: tx(strCache→系列名) / spPr(solidFill srgbClr→系列色) / smooth / marker(symbol,size)
  │         dLbls(showVal→label.show) / cat(strCache 或 numCache→类目) / val(numCache→数值, formatCode)
  ├ catAx(axId, title, majorGridlines, crossAx)                               ← 类目轴
  └ valAx ×2(axId, scaling min/max, title, majorGridlines, crossAx)           ← 左/右值轴
chart > legend(legendPos: r/l/t/b/tr)
chart > title(rich > p > r > t)
chart > dispBlanksAs (gap/span/zero)
```

**映射规则**：

| chart XML | ECharts |
|---|---|
| `lineChart` + `ser/smooth=1` | `series.type:"line", smooth:true`；smooth=0 → false |
| `areaChart` | `type:"line"` + `areaStyle:{}` |
| `barChart`（框架恒为 `barDir=col`） | `type:"bar"`（垂直柱） |
| `scatterChart` | `type:"scatter"` |
| `pieChart` | `type:"pie", radius:"60%"`，data=`[{name,value}]`；`varyColors` → ECharts 自动配色 |
| `bar3DChart`（`Default3dChartType`） | **降级为 `type:"bar"`**，JSON 加 `"degraded":"bar3d"` 标记（见 §7 决策点） |
| 双值轴：ser 的 `axId` 对到哪个 `valAx` | 第二个 valAx → `yAxis[1]`，对应 ser 加 `yAxisIndex:1`（框架固定：先左后右） |
| `valAx/scaling` 的 min/max | `yAxis.min/max` |
| `catAx/valAx` 的 `majorGridlines` | `splitLine.show` |
| 轴 `title` | `xAxis.name` / `yAxis.name` |
| `spPr/solidFill/srgbClr@val`（6 位 hex） | `series.itemStyle.color:"#xxxxxx"`（默认 GREEN=00B050 已写在 XML 里，直接读） |
| `marker/symbol`（circle/square/diamond/triangle/x） | `series.symbol`；无 marker → ECharts 默认 |
| `dLbls/showVal=1` | `series.label.show:true` |
| `legendPos` | t→top，b→bottom，l→left，r→right，tr→`{right:10,top:10}` |
| `title > rich > p > r > t` | `title.text`（rich 里 run 级样式 v1 可忽略，标注于文档） |
| `dispBlanksAs=gap` | `series.connectNulls:false`；span→true；zero→数据补 0 |
| `numCache/pt` 的 `formatCode` | `tooltip.valueFormatter`（可选） |

**option 骨架**：

```json
{
  "title": {"text": "示例"},
  "legend": {"bottom": 0, "data": ["系列A", "系列B"]},
  "grid": {"left": 45, "right": 55, "top": 30, "bottom": 30},
  "xAxis": [{"type": "category", "data": ["1月","2月"]}],
  "yAxis": [{"type": "value"}, {"type": "value"}],
  "series": [
    {"name": "系列A", "type": "line", "smooth": true, "itemStyle": {"color": "#00B050"}, "data": [1, 2]},
    {"name": "系列B", "type": "bar", "yAxisIndex": 1, "data": [3, 4]}
  ]
}
```

说明：Excel 图表的**绘图区留白、刻度密度由 ECharts 自适应**，与 Excel 原生渲染存在非像素级差异——这是图表还原的现实上限，验收口径=数据/类型/颜色/图例一致，不逐像素比对图表内部。

### 4.7 多 sheet

`exportTemplate`/`exportSheet` 每调用一次生成一个 sheet（同名自动 `_1` 后缀）。JSON 顶层用 `sheets: []`，每个 sheet 独立 `grid/cells/images/charts`。打印分页符读 `sheet.getRowBreaks()`（PageRowSplit 的产物）→ JSON `pageBreaks.rows`，渲染端映射为 `div style="break-after:page"`（仅打印场景有意义，屏幕渲染忽略）。

---

## 5. JSON Schema v1

```jsonc
{
  "version": "1.0.0",
  "generatedAt": "2026-10-09T15:00:00+08:00",
  "sheets": [
    {
      "name": "sheet",
      "grid": {
        "cols": [62.0, 40.375],          // 每列 px，浮点保留
        "rows": [26.6667, 26.6667]       // 每行 px
      },
      "cells": [                          // 只含"区域左上角"cell + 非合并独立 cell
        {
          "x": 0, "y": 0, "w": 3, "h": 1,           // 网格 span（调试友好）
          "rect": {"x": 0.0, "y": 0.0, "w": 164.375, "h": 26.6667},   // 像素矩形（渲染端唯一依据）
          "text": "1,234.56",                        // DataFormatter 显示串；空 cell 无此字段
          "value": 1234.56,                          // 可选：原始值（前端排序/交互用）
          "link": "https://...",                     // 可选
          "rich": [                                  // 可选：富文本 run（与 text 二选一优先 rich）
            {"text": "红", "css": {"color": "#FF0000"}},
            {"text": "字"}
          ],
          "css": {"fontFamily": "宋体", "fontSize": 14.67, "fontWeight": 700, "background": "#FFFF00",
                   "borderTop": "1px solid #000000", "justifyContent": "center"}
        }
      ],
      "images": [
        {"mime": "image/png", "data": "<base64>", "rect": {"x": 128.5, "y": 54.0, "w": 200, "h": 150}}
      ],
      "charts": [
        {"rect": {"x": 0, "y": 106.7, "w": 328, "h": 200}, "echarts": {"...option..."}, "degraded": null}
      ],
      "pageBreaks": {"rows": [12]}
    }
  ]
}
```

约定：坐标/尺寸单位一律 **px（CSS 像素，96dpi）**；浮点不取整；`rect` 是渲染唯一依据，`x/y/w/h` 网格值仅调试。

---

## 6. 前端渲染器规格（vanilla JS + ECharts，随 starter 打包为静态资源）

```
renderer/
  ├ report-renderer.js     // 唯一逻辑文件
  ├ report-renderer.css    // 基础样式
  └ echarts.min.js         // 唯一第三方依赖（豁免项）
```

渲染规则：

1. 容器：`position:relative; width:Σcols; height:Σrows; font-family 回退链 "宋体","SimSun",sans-serif`。
2. 每个 cell 一个绝对定位 div：`left/top/width/height = rect`；内部 `display:flex; justify-content:HAlign; align-items:VAlign`（比 table-cell 可控）。
3. 文本溢出：默认 `overflow:hidden`。Excel 对超宽数字显示 `###`，HTML 显示截断——已知差异，验收时豁免。
4. rotation：1–90° → `transform:rotate(-Ndeg)`；91–180° → `rotate(180-N deg)` + 反向；255 → `writing-mode:vertical-rl; text-orientation:upright`。
5. 图片：`<img>` 绝对定位，`object-fit:fill`（Excel anchor 即拉伸语义）。
6. 图表：容器 div 绝对定位 + `echarts.init(el).setOption(option)`；监听窗口 resize 时按 rect 重设（一般不需要，尺寸固定）。
7. gridlines 仿真（可选 P2）：未设背景色的 cell 画 Excel 式浅灰网格，开关属性。
8. 无任何构建步骤；页面 `<script>` 引入三个文件即可。

---

## 7. Starter 工程结构（Spring Boot）

```
excel-report-json-starter/
  ├ pom.xml                          // 依赖 excel-report + poi（框架传递）+ spring-boot-autoconfigure
  ├ src/main/java/io/github/mengfly/excel/report/json/
  │   ├ ExcelReportJsonAutoConfiguration.java
  │   ├ ExcelReportJsonProperties.java          // 前缀 excel-report.render
  │   ├ JsonReportService.java                  // 核心服务（§4 全部逻辑）
  │   ├ readback/                               // 读回子包：GeometryReader / CellReader / ImageReader / ChartReader
  │   └ web/JsonReportController.java           // 可选 web 层
  └ src/main/resources/
      └ META-INF/spring.factories               // 或 Boot3 的 AutoConfiguration.imports
```

**Properties**：

| 属性 | 默认 | 说明 |
|---|---|---|
| `excel-report.render.cache-templates` | `true` | true=按模板内容 sha256 缓存 `ReportTemplate` 并**对 render 串行化**（见下）；false=每次请求重新 parse |
| `excel-report.render.chart-3d` | `FLAT` | FLAT=降级 2D；SKIP=跳过 3D 图表（不输出） |
| `excel-report.render.image-inline-limit-kb` | `2048` | 超过则 `data` 置 null、另给 `/render/images/{id}` 引用 |
| `excel-report.render.default-font-family` | `宋体` | 前端回退链首位 |

**端点建议**：

| 方法/路径 | 语义 |
|---|---|
| `POST /report/render` | body=`{template, data}` → JSON（§5） |
| `POST /report/export` | 同入参 → xlsx 流（同一 `ExcelReport` 实例直接 `save`，不二次渲染） |
| `GET /report/preview` | 返回内置静态预览页（引 renderer 静态资源 + fetch `/report/render`） |

**线程安全（必须写进 starter 的 javadoc/README）**：
1. **同一个 `ReportTemplate` 实例并发 `render` 不安全且静默失败**（Xerces DOM 竞态被内部 catch 吞掉，产出缺内容——台账二.1 实测）。⇒ 缓存模板时必须对单实例 render 加锁（`synchronized(template)`），或干脆每次请求重新 parse（表达式缓存是静态共享的，parse 成本很低）。推荐后者起步，压测证明 parse 是瓶颈后再上缓存+锁。
2. `ExcelReport` 一请求一实例；`XSSFWorkbook` 禁止跨线程。
3. JSON 路径**不调 `save()`**，workbook 用完即弃（无临时文件残留）。

**Boot 版本**：框架是 JDK 8 编译 ⇒ Boot 2.7（JDK8/11）最稳；Boot 3（JDK17）运行期兼容，`spring.factories` 换 `AutoConfiguration.imports`。

---

## 8. 已知差异与验收清单

**已知差异（写进 README，验收豁免）**：
- 行内文本换行位置/字形（浏览器字体引擎差异）——网格几何不受影响；
- 数字超宽显示 `###` vs HTML 截断；
- 图表绘图区留白、刻度密度由 ECharts 自适应；
- 3D 图表降级为 2D（`charts.3d=FLAT`）；
- 主题色/索引色字体颜色回退（框架模板规范建议全部用命名色/HEX，规避）。

**验收方法（每个里程碑跑一遍）**：
1. 同一模板+数据走两条通道：`/report/export` 下载 xlsx 打开截图；`/report/preview` 截图；
2. 叠层比对：列边界、行边界、合并区边框、图片矩形应**完全重合**（同源保证）；
3. 用 `example/` 下现成模板（含 standard-table / grouped-report 两份技能正例）做回归集；
4. JSON 校验：`cells` 只含左上角 cell、`grid.cols.length` = 实际最大列、图片 `rect` 与 anchor 手算一致。

**实施里程碑**：

| 里程碑 | 内容 | 验收物 |
|---|---|---|
| M1 | 几何+文本+样式+合并区（单 sheet） | standard-table 模板 HTML vs Excel 叠层 |
| M2 | 图片（anchor 矩形 + base64） | 含图模板 |
| M3 | 图表映射（line/bar/area/scatter/pie + 双轴） | 含 Chart 模板 |
| M4 | RichText、超链接、多 sheet、pageBreaks | grouped-report + 富文本模板 |
| M5 | Starter 化（auto-config/properties/端点/静态资源） | 独立 Spring Boot 工程可引 |

---

## 9. 与路径 Y 的关系（预留）

本方案只依赖 §3.1 白名单，**不碰** `excel.*` 内部类。若将来按台账四.5 残余项落地 RenderContext 抽象（路径 Y），`JsonReportService` 的读回层可平移替换为 `JsonRenderContext` 实现，但**这不是本 starter 的前置条件**——X 与 Y 解耦，先跑通 X 不产生沉没成本。
