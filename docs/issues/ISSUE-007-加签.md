# ISSUE-007：加签 — 前/后/并行

- **优先级**：P0
- **模块**：任务中心 / 加签
- **负责人**：后端×2
- **预计工期**：W7
- **前置依赖**：ISSUE-005
- **里程碑**：M3

## 目标
支持审批过程中由**原审批人**临时增加审批人：前加签（先于自己）、后加签（后于自己）、并行加签（与自己同时），加签完成后返回原审批人继续。

## 范围
### In Scope
- 加签记录：`flow_add_sign_record`（Superpowers 计划 §2.6；TRD 任务类型=3）。
- 服务：`AddSignService.addSign / approveAddSign / rejectAddSign / getHistory`（TRD §4.2.2）。
- 发起 API：`POST /api/v1/tasks/{id}/add-sign`（TRD §3.3）。
- 三种类型：前/后/并行（PRD §加签模式）；加签完成后返回原审批人（TRD §4.2.2 `returnToOriginal`）。
- 约束：加签须原审批人发起（PRD §约束）。

### Out of Scope
- 会签（006）；任务基础操作（005）。

## 技术约束（以 TRD 为准）
- `AddSignHandler.initiateAddSign / approveAddSign / returnToOriginal`（TRD §4.2.2）。
- 加签任务状态与普通任务一致（wf_task）。

## 接口契约
- REST：`POST /api/v1/tasks/{id}/add-sign`。
- Java：`AddSignService`、`AddSignNodeHandler`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 005；对一条 userTask 任务分别发起前/后/并行加签，验证顺序与返回
```

### 验证用例（映射 PRD §任务中心 加签验收 / TRD §4.2.2）
- [ ] 前加签：被加签人先审批，再回到原审批人。
- [ ] 后加签：原审批人先审批，再交被加签人。
- [ ] 并行加签：二者均可处理，均完成后继续。
- [ ] 加签被拒绝：保持原任务，流程不异常。
- [ ] 非原审批人发起加签被拒（403）。
- [ ] 可查询加签历史（getHistory）。

### 验证脚本/测试
- `AddSignServiceTest`：三类型流转顺序。
- `AddSignApiTest`（MockMvc）：发起/审批/历史查询。

## 交付物
- 加签实体/服务/节点处理器/接口 + 历史查询。

## 验收门禁（DoD）
- 前/后/并行三种加签正确流转并回到原审批人；权限与历史查询生效。
