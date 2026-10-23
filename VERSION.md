## V1.6.1
[更新详情](version/V1.6.1.md)

### 新增
- 新增 `bridge` 导出能力：`WebJsonExporter` 把导出结果转成"前端可回显的扁平单元格网格"（`WebExcel`），
  含行列与合并跨度、归一化样式、内容（文本 / 富文本 / 图片 dataURL / 图表数据）以及列宽行高；
  `TextStructExporter` 输出组件树的文本结构，用于调试
- 导出入口 `exportTemplate` / `exportSheet` 返回 `ExportResult`（渲染后的组件树 + 导出上下文）
- 新增 `StyleAble#getFinalStyle()`：导出期间记录"祖先链 + 自身"合并后的完整样式
- `ReportContext` 暴露已算出的列宽 / 行高与 sheet 级样式

### 重构
- 文本组件拆分到 `component.text` 包，与其它组件保持一致
- 图表轴数据解析拆分出 `data.resolver` 包；`AxisType` 移入 `chart.axis`，`ChartMarker` / `ChartTitle` / `Legend` 移入 `chart.component`

### ⚠️ 破坏性变更
- 文本组件包迁移：`component.TextComponent` / `LinkComponent` / `SpanComponent` → `component.text.*`
- **`TableComponent` 语义替换（不一定能被编译器发现）**：原 `TableComponent`（`dataList` + `columns` 数据驱动表，自带导出逻辑）已删除，
  `TableComponentNew`（布局式，内部用 `GridLayout` 承载 `Text` 子组件）改名为 `TableComponent`。
  `new TableComponent(dataList, columns)` 这个签名在新旧两个类上都存在、代码仍可编译，但导出走的是标准组件链路；
  无参构造与 `setDataList` / `setColumns` / `setHeaderHeight` / `setHeaderVisible` 已不存在（字段改为 final）。
  模板标签 `<Table>` 指向新类，模板无需改动
- 图表包迁移：`chart.AxisType` → `chart.axis.AxisType`；`chart.data.{ChartMarker,ChartTitle}` 与 `chart.Legend` → `chart.component.*`；
  `chart.data.{AxisDataResolver,ValueAxisDataResolver,RelationAxisDataResolver}` → `chart.data.resolver.*`
- 移除 `Container#print()`、`Container#getTypeName()`、`Layout#print()`；需要打印组件树时改用 `TextStructExporter`

## V1.6.0
[更新详情](version/V1.6.0.md)

### 新增
- 新增模板校验能力：默认宽松模式（问题只在首次渲染时汇总成一条 WARN 日志），
  执行 `TemplateManager.getInstance().setStrict(true)` 切换为严格模式后，模板存在 ERROR 级问题时直接抛 `TemplateValidationException`
- `TemplateManager` 新增模板解析缓存，同一模板只解析一次；新增 `clearCache()` / `clearCache(id)` 用于清缓存
- 内置 XSD 补充单元格样式 key `fillBackgroundColor`

### 优化
- 表达式缓存改为全局共享，减少重复解析
- 样式链（StyleChain）重构，样式装配过程更轻量
- `width/height=auto` 的自动宽高计算延迟到组件终态统一进行（实测计算量约减少一半）

### 修复
- 修复同名 Sheet / 匿名 Sheet 超过 2 个时报 `The workbook already contains a sheet named ...` 的 bug
- 修复网络图片加载线程可能无限阻塞的问题
- 修复行高默认值失效的问题
- 修复模板 XSD 的语法错误及 `BeanUtil` 枚举名替换的正则错误

### ⚠️ 破坏性变更
- 包 `io.github.mengfly.excel.report.template.exepression` 更名为 `...template.expression`（拼写修正），引用该包的类型需要同步改 import
- 样式系统重构：`StyleKey` 构造与注册改为函数式（`targetType + styleType + applier`），移除 `CellWidthHeightKey`；
  `CellStyles` / `SheetStyles` 的样式应用统一为 `initStyle(target, styleMap)`（原 `createCellStyle` / `createFont` / `initSheetStyle` 已移除）
- `ContainerTreeNode#getSyleMap()` 更名为 `getStyleMap()`；`ReportTemplate` 不再持有 `TemplateManager`

## V1.5.1
- pref: 优化组件内部逻辑，便于后续功能扩展

## V1.5.0
- feat(layout): 添加网格布局组件支持
- feat(layout): 实现组件布局与测量机制
- feat(layout): 新增曲线图颜色配置项支持

## V1.4.0
- fix: 解决Text组件中表达式返回数字单元格提示为字符串的问题
- pref: 优化图表坐标轴标题设置，可不设置，在不设置的情况下不添加坐标轴标题
- feat: 添加自动调整组件大小功能
- fix: 解决 merge 失败问题
- feat: 数据支持Optional类型数据， 模板样式支持Id与编码的组合样式
- feat: 添加组件自动宽高功能支持
- fix: 解决解析表达式的死循环问题
- pref: 添加表达式解析日志
- refactor: 优化报表组件和样式处理
- refactor: 优化代码结构和类型安全
- feat(ImageComponent): 添加图片高度缩放功能

## V 1.3.0
[更新详情](version/V1.3.0.md)
- 解决图表样式设置无法生效的bug
- 解决base64图片无法显示的bug
- chart图表添加网格线支持
- chart图表支持标题样式设置
- 图表添加marker支持
- 解决固定样式的宽度设置时宽度和设定值不一致问题
- 取消曲线图边框
- 曲线图可设置是否平滑曲线
- 添加打印分页组件
- 图片添加padding属性

## V 1.2.0
[更新详情](version/V1.2.0.md)

- 添加 Template Manager, 以便支持模板引入
- 添加Include标签的支持
- 添加富文本标签的支持
- 优化图片显示，支持图片缩放样式设置，可设置居中显示等样式
- 优化Span边框显示，默认的Span边框为null
- 图片和图标类型可设置锚点样式，可设置是否随Cell大小移动和Resize
- 解决某些组件的width和height样式设置不起作用的bug

## V 1.1.0

- Container 中添加模板信息，方便调试与回显
- Span 缺失Xml的解析
- 解决 Excel 中无 Sheet 页时 Excel 无法打开的问题
- 修改 TextComponent 支持的数据类型为 Object

## V 1.0.0

- 组件 If 条件判断语句
- 布局 For 循环语句
- 将List的Header剥离出来，可以单独设置Style
- Cell DataFormat功能
- 图表数据功能
    - 折线图（二维/三维）
    - 柱状图（二维/三维）
    - 饼图（二维/三维）
    - 散点图（二维/三维）
- 模板文件 Schema xsd 文件编写
- 补源码注释
- 添加LinkComponent 