# ISSUE-029 规则引擎使用说明

> 配套文档：`ISSUE-029-状态机与规则引擎基础底座.md`
> 适用范围：后端 `flow-engine` 模块、前端 `flow-web` 模块
> 关键词：规则引擎、SpEL、QLExpress、闸门校验、可配置热更

---

## 1. 概述

规则引擎是 ISSUE-029 基础设施底座提供的四类可复用能力之一（其余为状态机、编号引擎、公式计算引擎）。它解决"业务规则可配置、可热更新、执行时统一求值"的问题，典型场景：

- **资质闸门**：派单前校验采样人员资质、仪器是否可用。
- **物资校准闸门**：使用仪器/物资前校验其校准有效期。
- **超标判定**：检测结果是否超过限值（`value > limit`）。
- **质控判定**：质控活动偏差是否在允许范围内。

### 1.1 技术选型说明

设计文档原定使用 **QLExpress**，实际落地采用 **Spring EL（SpEL）** 作为等价的沙箱表达式引擎（见 `ExprEngine.java`）。两者语义相近，规则以字符串表达式存储，上下文参数以 `#name` 引用。选择 SpEL 的原因：

- 与 Spring 生态无缝集成，无需额外依赖；
- 沙箱化执行，满足 BR-029-05（禁止危险 API 调用）；
- 表达式语法开发者熟悉，降低维护成本。

> 文档与代码差异提示：本说明以**实际代码（SpEL）**为准。

---

## 2. 核心组件

| 组件 | 位置 | 职责 |
|------|------|------|
| `EmsRuleDef` | `flow-engine/.../entity/EmsRuleDef.java` | 规则定义实体（映射 `t_rule_def`） |
| `EmsRuleEngineService` | `flow-engine/.../service/EmsRuleEngineService.java` | 规则求值 / 列表 / 保存 / 删除 |
| `ExprEngine` | `flow-engine/.../util/ExprEngine.java` | 底层 SpEL 沙箱执行（布尔 / 对象 / 数值） |
| `EmsBaseController` | `flow-engine/.../controllers/EmsBaseController.java` | HTTP 接口（`/api/v1/rule/*`） |
| `RuleAdmin.vue` | `flow-web/src/views/ems/base/RuleAdmin.vue` | 前端规则配置后台 + 调试 |
| `DatabaseMigration` | `flow-engine/.../config/DatabaseMigration.java` | 内置规则幂等初始化 |

---

## 3. 数据模型（`t_rule_def`）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| rule_key | TEXT | 规则唯一标识（如 `dispatch_gate`），代码按此调用 |
| rule_name | TEXT | 规则名称（展示用，如 `派单资质闸门`） |
| expr | TEXT | SpEL 表达式，上下文变量以 `#var` 引用 |
| enabled | INTEGER | 1 启用 / 0 停用（停用则求值恒为 false） |
| version | INTEGER | 版本号，`save` 时自动 +1 |
| remark | TEXT | 说明 |
| create_time / update_time | TEXT | 时间戳 |

---

## 4. 内置规则（随数据库迁移自动初始化）

`DatabaseMigration.initRuleDefs()` 幂等插入以下规则（已存在于库中不重复插入）：

| rule_key | rule_name | 表达式 | 含义 | 上下文变量 |
|----------|-----------|--------|------|-----------|
| `dispatch_gate` | 派单资质闸门 | `#staffQualified == true && #instAvailable == true` | 人员资质合格且仪器可用 | `staffQualified`, `instAvailable` |
| `material_calib_gate` | 物资校准闸门 | `#calibrated == true` | 物资/仪器已校准 | `calibrated` |
| `exceed_judge` | 超标判定 | `#value > #limit` | 检测值超过限值 | `value`, `limit` |
| `qc_pass` | 质控判定 | `#deviation <= #allowDeviation` | 偏差在允许范围内 | `deviation`, `allowDeviation` |

---

## 5. 后端使用

### 5.1 注入与调用

```java
@Service
public class SomeBizService {
    private final EmsRuleEngineService ruleEngine;

    public SomeBizService(EmsRuleEngineService ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public void dispatch(String bizId, Map<String, Object> ctx) {
        // ctx 含 staffQualified / instAvailable
        boolean ok = ruleEngine.eval("dispatch_gate", ctx);
        if (!ok) {
            throw new IllegalStateException("派单资质闸门未通过");
        }
        // 继续派单逻辑 ...
    }
}
```

**求值语义（关键）：**

- 规则不存在、未启用（`enabled=0`）、或表达式执行异常 → `eval()` 返回 `false`。
- 设计意图：**默认拒绝（fail-safe）**，调用方需在返回 `false` 时阻断业务。

### 5.2 底层引擎 `ExprEngine`（直接用于公式/守卫）

- `evalBoolean(expr, ctx)` → 布尔，异常返回 `false`（用于闸门/守卫）；
- `eval(expr, ctx)` → 对象（用于公式计算，异常上抛）；
- `evalNumber(expr, ctx)` → 数值（用于单位换算/检测公式）。

上下文注入方式（SpEL 变量）：

```java
Map<String, Object> ctx = new HashMap<>();
ctx.put("staffQualified", true);
ctx.put("instAvailable", true);
boolean pass = ExprEngine.evalBoolean("#staffQualified == true && #instAvailable == true", ctx);
```

> 注意：表达式中的变量必须以 `#` 前缀引用（如 `#value`）。

### 5.3 与状态机联动（Guard）

状态机迁移的 `t_transition_def.guard_expr` 字段直接存放 SpEL 表达式，由 `EmsStateMachineService.fire()` 在迁移前调用 `ExprEngine.evalBoolean(guardExpr, ctx)` 执行（BR-029-02）。Guard 失败则迁移被拒绝并返回 `guardFailMsg`。

---

## 6. 前端使用

### 6.1 API 封装（`flow-web/src/api/ems.js`）

```js
import { evalRule, listRules, saveRule, removeRule } from '@/api/ems'

// 规则求值
const res = await evalRule('dispatch_gate', { staffQualified: true, instAvailable: true })
// res.data.result → true / false

// 规则列表 / 保存 / 删除
const list = await listRules()              // res.data: EmsRuleDef[]
await saveRule({ ruleKey, ruleName, expr, remark, enabled: 1 })
await removeRule(id)
```

### 6.2 规则配置后台（`RuleAdmin.vue`）

路径：`/ems/base/rule-admin`（菜单：基础设施底座 → 规则引擎配置）。

功能：

- **规则列表**：展示全部规则（标识 / 名称 / 表达式 / 启用状态），支持编辑、删除。
- **新增/编辑**：填写 `ruleKey`、`ruleName`、SpEL 表达式、`remark`，切换启用开关。
- **规则调试**：选择规则 + 输入上下文 JSON（如 `{"staffQualified":true,"instAvailable":true}`），点击「求值」即时返回结果，便于上线前验证表达式。

### 6.3 接口一览（`/api/v1`）

| 方法 | 路径 | 说明 | 请求体/参数 |
|------|------|------|------------|
| POST | `/rule/eval` | 规则求值 | `{ ruleKey, context }` |
| GET | `/rule` | 规则列表 | — |
| POST | `/rule` | 保存规则 | `EmsRuleDef` 字段 |
| DELETE | `/rule/{id}` | 删除规则 | — |

> 以上接口均经 `@OpLog` 切面记录到 `sys_operation_log`（WORM 审计，复用 ISSUE-014），满足 BR-029-07。

---

## 7. 编写规则表达式规范

1. 上下文变量统一以 `#` 前缀引用（如 `#value`）。
2. 表达式返回类型应为布尔（闸门场景）；公式计算场景可返回数值/对象。
3. 保持幂等、无副作用；不要调用 I/O 或静态可变状态（沙箱约束 BR-029-05）。
4. 命名 `rule_key` 全小写 + 下划线（如 `dispatch_gate`），代码按 key 引用。
5. 上线前用「规则调试」验证；表达式异常在 `evalBoolean` 下会静默返回 `false`，需谨慎。

**示例：新增一个"报告审批权限闸门"**

```sql
INSERT INTO t_rule_def(rule_key, rule_name, expr, enabled, version, remark, create_time, update_time)
VALUES('report_approve_gate', '报告审批权限闸门',
       "#role == 'reviewer' && #reportStatus == '待审核'", 1, 1,
       '仅复核角色可对待审核报告审批', datetime('now'), datetime('now'));
```

```java
boolean canApprove = ruleEngine.eval("report_approve_gate",
    Map.of("role", currentRole, "reportStatus", status));
```

---

## 8. 验证与测试

- 后端冒烟：`flow-engine/smoke-issue029.cjs` 覆盖规则求值（通过/拒绝）、规则列表、操作审计写入。
- 前端 UI 测试：`flow-web/ui-test-issue029.cjs` 覆盖规则后台渲染、调试求值流程。
- 运行后端冒烟示例：

```bash
node flow-engine/smoke-issue029.cjs   # 需后端运行于 :8080，且已登录可用账号(sys_admin/admin123)
```

---

## 9. 注意事项 / 已知约束

- **fail-safe 默认拒绝**：规则缺失/停用/表达式异常时 `eval()` 返回 `false`，调用方务必据此阻断，避免"规则没配就放行"。
- **与状态机 Guard 共用同一 SpEL 引擎**：`t_transition_def.guard_expr` 与 `t_rule_def.expr` 语法一致。
- **热更新**：修改 `t_rule_def.expr` 后，下次 `eval()` 直接读取最新值，无需重启（配置热更，BR-029-03）。
- **安全**：当前为应用内 SpEL 沙箱，未启用更严格的白名单函数限制；编写表达式时应仅使用布尔/算术运算，避免引入 `T(...)` 类型引用等高危语法。
