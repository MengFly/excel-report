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