# ISSUE-022 前端开发计划（环境监测 LIMS 基础数据管理）

> 关联：ISSUE-022（后端已完成：10 张 `t_` 表 + 9 Service + 2 Controller，接口 `/api/v1/ems/base/*`、`/api/v1/ems/shared/*`，单测/接口E2E 全通过）
> 原则：**尽可能复用现有前端能力，不重复造轮子**。本计划只新增最小必要文件，复用项见第 2 章。
> Agent Loop 阶段：需求澄清 ✅ → 制定计划（本文件）→ 编码开发 → 单元测试 → 自动化测试 → 更新状态

---

## 1. 目标与范围

为 ISSUE-022 的 10 个后端实体提供前端页面，复用既有 Ant Design Vue 技术栈（`a-table`/`a-modal`/`a-form`/`a-tag`/`a-popconfirm` + `hasPerm` 按钮权限）。

| 页面 | 对应后端接口 | 优先级 | 复用模板 |
|------|--------------|--------|----------|
| 客户管理 | `/ems/base/customers` | P0 | `system/user.vue`（表格+弹窗） |
| 监测点位 | `/ems/base/points` | P0 | `system/user.vue` |
| 委托草稿 | `/ems/base/entrusts` | P1 | `system/user.vue` |
| 部门管理 | `/ems/base/departments` | P1 | `system/dept.vue`（树+弹窗） |
| 数据字典 | `/ems/base/dicts` | P0 | `system/dict.vue`（左类型+右项） |
| 集成配置 | `/ems/base/integration-cfg` | P1 | `system/user.vue` |
| 采样车辆 | `/ems/base/vehicles` | P2 | `system/user.vue` |
| 文件管理 | `/ems/shared/files` | P2 | `system/log.vue`（只读列表） |
| 预警中心 | `/ems/shared/alerts` | P2 | `system/log.vue` |
| 站内信 | `/ems/shared/messages` | P2 | `system/log.vue` |

---

## 2. 可复用的现有能力（不重复造轮子）

| 能力 | 现有位置 | 复用方式 |
|------|----------|----------|
| **HTTP 封装** | `src/api/request.js` | 直接 `import request`，baseURL=`/api/v1`，已处理 `{code,data,message}` 与异常提示；新接口只需 `request.get('/ems/base/customers')` |
| **权限指令** | `src/composables/usePermission.js` → `hasPerm('xxx:create')` | 所有页面按钮沿用 `v-if="hasPerm('ems:customer:create')"` |
| **菜单渲染** | `src/layouts/BasicLayout.vue` 的 `menuConfig`（硬编码数组，按 `hasPermission` 过滤） | 仅在 `menuConfig` **追加**一个 `ems` 父节点 + 子项，菜单组件无需改动 |
| **路由范式** | `src/router/index.js`（`BasicLayout` 子路由 + `meta.permKey`） | 新增 `/ems/*` 子路由，沿用现有守卫（自动按 permKey 鉴权） |
| **页面样式** | `system/dict.vue` 中的 `.page-wrap/.card-wrap/.page-header/.action-link/.active-item` | 直接复用同款 class 与布局栅格 |
| **CRUD 交互范式** | `system/dict.vue` / `system/dept.vue` / `system/user.vue` | 复制「列表表格 + 新建/编辑 Modal + Popconfirm 删除 + 状态 Tag」结构，仅替换字段与接口 |
| **状态标签** | `dict.vue` 的 `<a-tag :color>` 写法 | 复用展示 `status` 启用/停用 |

> 结论：**无需新建通用组件、无需新封装 request、无需改权限体系**。所有页面均为「复制既有页面骨架 → 改字段/接口/权限 key」的增量工作。

---

## 3. 需要新增/修改的文件清单

### 3.1 新增 API 模块（1 个，集中收敛接口）
- `src/api/ems.js`
  ```js
  import request from './request'
  // 基础数据
  export const getCustomers = (params) => request.get('/ems/base/customers', { params })
  export const createCustomer = (data) => request.post('/ems/base/customers', data)
  export const disableCustomer = (id) => request.post(`/ems/base/customers/${id}/disable`)
  export const getPoints = (params) => request.get('/ems/base/points', { params })
  export const createPoint = (data) => request.post('/ems/base/points', data)
  export const getEntrusts = (params) => request.get('/ems/base/entrusts', { params })
  export const createEntrust = (data) => request.post('/ems/base/entrusts', data)
  export const getDepartments = () => request.get('/ems/base/departments')
  export const createDepartment = (data) => request.post('/ems/base/departments', data)
  export const getDictsByType = (type) => request.get(`/ems/base/dicts/type/${type}`)
  export const createDict = (data) => request.post('/ems/base/dicts', data)
  export const upsertCfg = (data) => request.post('/ems/base/integration-cfg', data)
  export const getCfgPlain = (key) => request.get(`/ems/base/integration-cfg/${key}/plain`)
  export const getVehicles = () => request.get('/ems/base/vehicles')
  export const createVehicle = (data) => request.post('/ems/base/vehicles', data)
  // 共享实体
  export const getFiles = (params) => request.get('/ems/shared/files', { params })
  export const archiveFile = (data) => request.post('/ems/shared/files', data)
  export const getAlerts = (params) => request.get('/ems/shared/alerts', { params })
  export const pushAlert = (data) => request.post('/ems/shared/alerts', data)
  export const getMessages = (params) => request.get('/ems/shared/messages', { params })
  export const sendMessage = (data) => request.post('/ems/shared/messages', data)
  ```

### 3.2 新增页面（按优先级，建议分 3 批）
| 批 | 文件 | 数量 |
|----|------|------|
| 批1（P0） | `views/ems/base/Customer.vue`、`views/ems/base/Point.vue`、`views/ems/base/Dict.vue` | 3 |
| 批2（P1） | `views/ems/base/Entrust.vue`、`views/ems/base/Department.vue`、`views/ems/base/IntegrationCfg.vue` | 3 |
| 批3（P2） | `views/ems/base/Vehicle.vue`、`views/ems/shared/File.vue`、`views/ems/shared/Alert.vue`、`views/ems/shared/Message.vue` | 4 |

> 每页结构：`template`（a-table + a-modal 表单 + 权限按钮）/ `script setup`（import ems.js + usePermission，沿用 dict.vue 写法）/ 复用 `.page-wrap` 样式。

### 3.3 修改现有文件（仅 2 处，最小改动）
1. `src/router/index.js`：在 `BasicLayout` 的 `children` 中追加 `/ems` 子路由（沿用 `meta.permKey` 范式）。
   ```js
   {
     path: 'ems/base/customer', name: 'EmsCustomer',
     component: () => import('../views/ems/base/Customer.vue'),
     meta: { title: '客户管理', permKey: 'ems:customer' }
   },
   // …其余 9 个同理（point/dict/entrust/department/integration-cfg/vehicle/shared-file/shared-alert/shared-message）
   ```
2. `src/layouts/BasicLayout.vue`：在 `menuConfig` 追加 `ems` 父节点（含上述子菜单），菜单组件自动按权限渲染，无需改渲染逻辑。
   ```js
   {
     key: 'ems', title: '环境监测LIMS', permKey: 'ems',
     icon: markRaw(EnvironmentOutlined),
     children: [
       { key: 'ems-customer', title: '客户管理', path: '/ems/base/customer', permKey: 'ems:customer' },
       { key: 'ems-point', title: '监测点位', path: '/ems/base/point', permKey: 'ems:point' },
       { key: 'ems-dict', title: '数据字典', path: '/ems/base/dict', permKey: 'ems:dict' },
       { key: 'ems-entrust', title: '委托草稿', path: '/ems/base/entrust', permKey: 'ems:entrust' },
       { key: 'ems-department', title: '部门管理', path: '/ems/base/department', permKey: 'ems:department' },
       { key: 'ems-cfg', title: '集成配置', path: '/ems/base/integration-cfg', permKey: 'ems:integration-cfg' },
       { key: 'ems-vehicle', title: '采样车辆', path: '/ems/base/vehicle', permKey: 'ems:vehicle' },
       { key: 'ems-file', title: '文件管理', path: '/ems/shared/file', permKey: 'ems:file' },
       { key: 'ems-alert', title: '预警中心', path: '/ems/shared/alert', permKey: 'ems:alert' },
       { key: 'ems-message', title: '站内信', path: '/ems/shared/message', permKey: 'ems:message' }
     ]
   }
   ```
   > 注意：`BasicLayout.vue` 图标为**具名 import**（第107行 `SettingOutlined` 等），需在其图标 import 区追加一行 `EnvironmentOutlined,`（来自 `@ant-design/icons`，属已安装依赖，无新增包）。

---

## 4. 权限 key 约定（对接 ISSUE-030 RBAC）

沿用 `system:xxx:create` 三段式，替换为 `ems:` 前缀：
`ems:customer`、`ems:customer:create`、`ems:point`、`ems:point:create`、`ems:dict`、`ems:dict:create` … 与 `meta.permKey` 及 `menuConfig.permKey` 一一对应。后端无需新增接口，前端权限由 ISSUE-030 的 RBAC 分配。

---

## 5. 测试与验收

| 类型 | 方式 | 说明 |
|------|------|------|
| 编译 | `npm run build` / `vite build` | 确认 10 页 + 路由 + 菜单无报错 |
| 接口联调 | 起 `flow-engine`（已完成）调 `/api/v1/ems/*` | 复用后端已通过的 18 个测试，前端仅调同接口 |
| 权限验证 | 用无 `ems:*` 权限账号登录 | 菜单与按钮应自动隐藏（得益于 `hasPermission` 过滤） |
| UI 自动化 | 可选：Playwright 走查 P0 三页（客户/点位/字典）增删改查 | 复用既有 E2E 手段（如有） |

**验收门禁（DoD）**
- [ ] 菜单出现「环境监测LIMS」且按权限显示子项
- [ ] P0 三页（客户/点位/字典）可完成增删改查，状态 Tag 正常
- [ ] 按钮按 `hasPerm` 显隐正确
- [ ] `npm run build` 通过
- [ ] 与后端 18 接口 E2E 对齐，无 404/500

---

## 6. 工作量估算

| 项 | 估算 |
|----|------|
| `ems.js` API 模块 | 0.5h |
| 3 个 P0 页面（复制 dict/user 骨架改字段） | 3h |
| 3 个 P1 页面 | 3h |
| 4 个 P2 页面 | 3h |
| 路由 + 菜单接入 | 0.5h |
| 联调 + 构建验证 | 1h |
| **合计** | **约 14h（前端增量，复用为主）** |

> 复用度：约 70% 代码来自复制现有页面骨架（dict/user/dept/log），仅改字段、接口路径、权限 key，几乎不写新通用逻辑。

---

## 7. 执行记录（2026-08-01，P0 批次）

### 7.1 已交付（编码阶段完成）
- `src/api/ems.js` —— 20 个接口封装（基础数据 + 共享实体）
- `src/views/ems/base/Customer.vue` —— 客户管理（复制 `system/user.vue` 骨架，字段对应 `EmsCustomer`，状态 Tag + 停用 Popconfirm）
- `src/views/ems/base/Point.vue` —— 监测点位（含经纬度 `a-input-number` 范围校验、客户下拉）
- `src/views/ems/base/Dict.vue` —— 数据字典（复制 `system/dict.vue` 左类型右项双栏，类型本地种子 + `getDictsByType` 加载项）
- `src/router/index.js` —— 追加 `/ems/base/customer|point|dict` 三条子路由（`meta.permKey` 范式）
- `src/layouts/BasicLayout.vue` —— `menuConfig` 追加「环境监测LIMS」父节点 + `EnvironmentOutlined` 图标 import
- 全部通过 IDE eslint/类型 lint（无语法/类型错误）

### 7.2 自动化测试（构建验证）状态：⛔ 环境阻塞
本机 `npm install` 无法完成，导致 `vite build` 无法运行：
1. 全局 `.npmrc` 锁定 `registry.npmmirror.com`，其未实现 `/-/npm/v1/security/audits/quick`，触发 **npm 10.8.2 `Exit handler never called` 崩溃**（`node_modules` 仅生成空壳目录）。
2. 改用官方源 `registry.npmjs.org` 安装虽跑完，但 `node_modules/vite` 为空壳（依赖未真正 reify 完整），`vite build` 报 `'vite' 不是内部或外部命令` / `Cannot find module vite/bin/vite.js`。

**该阻塞与代码正确性无关**，纯属依赖安装环境问题。可复现修复命令（环境就绪后）：
```bash
cd flow-web
# 方案A：用 npm 8（不调用崩溃的 audit endpoint）配合 npmmirror
nvm use 16 && npm install && npm run build
# 方案B：官方源 + 关闭 audit
npm install --registry=https://registry.npmjs.org/ --no-audit --no-fund && npm run build
```

### 7.3 验收门禁（DoD）当前状态
- [x] P0 三页代码完成，严格复用现有范式（dict/user/dept/log + request/usePermission/menuConfig）
- [x] 路由 + 菜单接入完成，权限 key `ems:customer|ems:point|ems:dict` 与 ISSUE-030 RBAC 对齐
- [x] 静态 lint 通过
- [ ] `npm run build` 通过 —— **待依赖安装环境就绪后执行**
- [ ] 与后端 18 接口 E2E 联调 —— 待构建后浏览器验证
