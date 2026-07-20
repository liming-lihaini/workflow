## 一、文档说明

1. 适用范围：PC 端管理系统，包含**登录页、首页、明细详情页**三大核心页面
2. 技术基准：Vue3 + Element Plus / Ant Design Vue，统一 CSS 变量管理样式
3. 统一约束：布局、色值、按钮、表单、卡片、字体、间距、交互统一，全局复用
4. 全局样式采用 CSS Variable 统一定义，禁止页面硬编码色值、尺寸

# 二、全局基础设计规范（所有页面通用）

## 2.1 全局色彩体系

### 主色（品牌色）

```
--color-primary: #1677ff; /* 主按钮、高亮、选中、导航激活 */
--color-primary-light: #e6f0ff;
--color-primary-dark: #0d5bcc;
```

### 辅助状态色

```
--color-success: #00b42a; /* 成功、通过 */
--color-warning: #ff7d00; /* 警告、待处理 */
--color-danger: #f53f3f; /* 删除、取消、异常 */
--color-info: #86909c; /* 次要提示、置灰 */
```

### 文字色

```
--text-title: #1d2129; /* 标题大文字 */
--text-content: #4e5969; /* 正文常规文字 */
--text-placeholder: #86909c; /* 输入框占位、次要说明 */
--text-disable: #c9cdd4; /* 禁用文字 */
```

### 背景色

```
--bg-page: #f5f7fa; /* 页面全局背景（首页/明细页） */
--bg-card: #ffffff; /* 卡片、弹窗、表单容器背景 */
--bg-login: linear-gradient(135deg, #1677ff 0%, #0958d9 100%); /* 登录页渐变背景 */
--bg-hover: #f2f3f5; /* 行hover、按钮hover浅色 */
--bg-disabled: #f2f3f5;
```

### 分割线 / 边框

```
--border-base: #dcdfe6;
--border-light: #e5e6eb;
--radius-sm: 4px;
--radius-md: 6px;
--radius-lg: 8px;
```

## 2.2 间距规范（统一单位 px）

```
--gap-xs: 4px;
--gap-sm: 8px;
--gap-md: 16px;
--gap-lg: 24px;
--gap-xl: 32px;
```

## 2.3 字体规范

- 全局字体：`Inter, "Microsoft YaHei", sans-serif`
- 标题：16px / 18px / 20px，字重 600
- 正文：14px，常规字重 400
- 辅助小字：12px

## 2.4 按钮统一规范

### 1）主按钮（primary）

- 背景：`var(--color-primary)`
- 文字：#fff
- 圆角：`var(--radius-md)`
- 高度：40px
- hover：`var(--color-primary-dark)`
- 适用：登录、查询、保存、提交

### 2）次按钮（default）

- 背景：#fff
- 边框：`var(--border-base)`
- 文字：`var(--text-content)`
- hover：背景 `var(--bg-hover)`
- 适用：重置、导出、查看

### 3）危险按钮（danger）

- 背景：`var(--color-danger)`
- 适用：删除、批量删除

### 4）文字按钮（text）

- 无背景无边框，文字主色
- 适用：列表操作、详情跳转

### 按钮尺寸统一

- 大号：40px（表单弹窗、登录）
- 常规：36px（页面操作栏）
- 小号：32px（表格内操作）

## 2.5 卡片布局通用规则

1. 卡片背景：`var(--bg-card)`
2. 圆角：`var(--radius-lg)`
3. 阴影：`0 1px 4px rgba(0,0,0,0.08)`
4. 内边距：`var(--gap-lg)`
5. 卡片标题与内容间距：`var(--gap-md)`

# 三、登录页规范

## 3.1 页面整体布局

1. 页面全屏布局，flex 垂直水平居中
2. 背景：渐变蓝 `var(--bg-login)`，禁止纯色
3. 左右分栏布局（可选）：
   - 左侧：品牌宣传区（占 55% 宽度），白色文字，居中展示 logo + 标语
   - 右侧：登录表单卡片（占 45% 宽度，最大宽度 420px）
4. 移动端自动切换为上下布局，全屏居中表单

## 3.2 登录卡片样式

- 卡片背景：#fff
- 圆角：`var(--radius-lg)`
- 内边距：上下 40px，左右 32px
- 卡片标题：20px，主色，居中，底部间距 24px
- 输入框高度：40px，圆角`var(--radius-sm)`
- 输入框间距：16px
- 登录按钮宽度：100%，高度 40px，主按钮样式
- 底部小字（忘记密码 / 注册）：12px，文字按钮，居中

## 3.3 登录页禁用规则

1. 不使用花哨动效，仅 hover 轻微变色
2. 表单校验提示文字红色 12px，距输入框 4px
3. 页面无多余弹窗、悬浮广告类元素

# 四、主页（系统首页 / 工作台）规范

## 4.1 整体页面布局结构（经典后台三栏）

```
┌─────────────────────────────────────┐
│ 顶部导航 Header                      │
├──────────┬──────────────────────────┤
│          │                          │
│ 侧边栏   │ 主内容区 Main             │
│ Sidebar  │                          │
│          │                          │
└──────────┴──────────────────────────┘
```

1. 页面整体背景：`var(--bg-page)`
2. 顶部 Header
   - 高度：60px
   - 背景：#fff
   - 底部 1px 分割线 `var(--border-light)`
   - 左侧 logo，右侧用户信息、系统设置、退出（文字按钮 / 头像下拉）
3. 侧边栏 Sidebar
   - 宽度固定 220px，折叠后 64px
   - 背景：#001529
   - 菜单文字：#bfcbd9，激活菜单 `var(--color-primary)` 左侧高亮条
4. 主内容区域
   - 外边距：16px
   - 所有模块均使用卡片包裹，不直接裸露文字在页面底色上

## 4.2 首页模块布局规范

1. 顶部数据统计模块（多卡片横向平铺）
   - 单卡片宽度均分，最小宽度 220px
   - 内部左图标 + 右数字文字布局
   - 数字大号 24px 主色，标题 14px 次要文字
2. 功能快捷入口（网格布局）
   - 每行 4/6 个图标按钮，间距 16px
   - 图标居中，下方文字说明，hover 轻微上浮阴影
3. 待办 / 通知列表卡片
   - 列表单行高度 48px
   - 左侧状态标记圆点，右侧时间小字
4. 页面操作栏（卡片顶部）
   - 左：标题（18px 600）
   - 右：按钮组（查询、新增、导出、批量操作）
   - 按钮之间间距 8px

## 4.3 表格统一规则（首页列表核心）

1. 表格外层卡片包裹
2. 表头背景：#f7f8fa
3. 行 hover：`var(--bg-hover)`
4. 操作列统一文字按钮，间距 8px
5. 分页放表格底部右侧，距离表格 16px

# 五、明细详情页规范

## 5.1 页面整体布局

1. 面包屑导航置顶，距离下方卡片 16px
2. 页面背景：`var(--bg-page)`
3. 顶部操作区：返回、编辑、删除、导出按钮组（靠右）
4. 内容分多块卡片展示，卡片之间间距 24px

## 5.2 基础信息卡片（顶部详情卡片）

1. 卡片标题 18px 加粗，底部 1px 分割线，下方间距 16px
2. 信息采用**两列 / 四列栅格布局**
   - label：右对齐，文字`var(--text-placeholder)`
   - value：左对齐，`var(--text-content)`
3. 单行高度 36px，上下间距 12px
4. 长文本、备注单独占整行展示

## 5.3 附属子表格卡片（明细流水、记录）

1. 独立卡片，标题区分模块（操作日志、订单流水、附件）
2. 表格规则同首页表格规范
3. 无数据时居中展示空状态图 + 提示文字

## 5.4 表单编辑弹窗（明细页编辑弹出）

1. 弹窗宽度：680px（常规），大表单 900px
2. 弹窗内边距 24px
3. 表单标签宽度统一 120px
4. 表单项间距 16px
5. 底部按钮靠右，取消（次按钮）+ 保存（主按钮）

# 六、全局交互统一规范

1. 所有按钮点击有轻微缩放 `transform: scale(0.98)`
2. 输入框聚焦边框变色为主色
3. 弹窗遮罩透明度 0.6，居中弹出
4. 加载状态统一使用全局 loading，禁止自定义加载样式
5. 操作成功 / 失败提示统一全局 message，不使用 alert

# 七、CSS 变量统一引入示例（可直接复制）

```
:root {
  /* 主色 */
  --color-primary: #1677ff;
  --color-primary-light: #e6f0ff;
  --color-primary-dark: #0d5bcc;
  --color-success: #00b42a;
  --color-warning: #ff7d00;
  --color-danger: #f53f3f;
  --color-info: #86909c;

  /* 文字 */
  --text-title: #1d2129;
  --text-content: #4e5969;
  --text-placeholder: #86909c;
  --text-disable: #c9cdd4;

  /* 背景 */
  --bg-page: #f5f7fa;
  --bg-card: #ffffff;
  --bg-login: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  --bg-hover: #f2f3f5;
  --bg-disabled: #f2f3f5;

  /* 边框圆角 */
  --border-base: #dcdfe6;
  --border-light: #e5e6eb;
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 8px;

  /* 间距 */
  --gap-xs: 4px;
  --gap-sm: 8px;
  --gap-md: 16px;
  --gap-lg: 24px;
  --gap-xl: 32px;
}
```

# 八、开发强制约束（编码规范）

1. 所有颜色、尺寸、圆角、间距必须使用 CSS 变量，禁止写死 #xxx、px
2. 页面结构统一使用 `div class="page-wrap"` 最外层容器
3. 卡片统一 class `card-wrap`，复用全局卡片样式
4. 按钮统一使用组件封装，不原生写 button 样式
5. 页面结构分层：面包屑 → 操作栏 → 卡片模块，顺序不可打乱
6. 适配：最小宽度 1200px，低于宽度出现横向滚动条，不挤压布局