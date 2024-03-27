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