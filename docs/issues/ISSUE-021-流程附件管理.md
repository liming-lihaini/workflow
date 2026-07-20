# ISSUE-021：流程附件管理 — 本地 / 阿里云 OSS 存储切换 + 下载 + 预览

- **优先级**：P1
- **模块**：附件管理
- **负责人**：后端×1
- **预计工期**：W8
- **前置依赖**：ISSUE-002（公共模块/异常/工具，提供 FlowException、Result 基础）；ISSUE-013（登录鉴权上下文，用于下载/预览鉴权）
- **后续集成**：ISSUE-008 表单引擎（文件组件对接本 Issue 的上传/元数据接口）
- **里程碑**：M3
- **状态**：📋 待开发

## 目标
为流程提供统一的附件能力：文件上传后以**本地磁盘**或**阿里云 OSS** 两种存储后端之一持久化，通过配置即可**零代码切换**；并对外提供**附件下载**与**在线预览**（图片/PDF 内联）能力。存储细节对上层业务透明，流程/表单只依赖统一的 `StorageProvider` 抽象与附件元数据服务。

## 范围
### In Scope
- **存储抽象层**：`StorageProvider` 接口，统一 `put / get / delete / generatePresignedUrl` 语义，与具体存储解耦。
- **本地存储实现** `LocalStorageProvider`：文件落到可配置本地目录（`storage.local.base-path`），以本地文件流响应下载/预览。
- **阿里云 OSS 实现** `OssStorageProvider`：集成 `com.aliyun.oss:aliyun-sdk-oss`，按 `endpoint/bucket/accessKey/region` 配置；下载/预览优先使用 OSS 预签名 URL（或流式代理）。
- **存储切换**：`application.yml` 的 `storage.type=local|oss` + Spring `@ConditionalOnProperty` 自动装配，运行时切换无需改代码；默认 `local`，无 OSS 凭证也可独立验证。
- **附件元数据表** `wf_attachment`（新增，TRD §2.1.8 扩展）：`attachment_id`、`process_instance_id`、`task_id`、`field_key`、`file_name`、`content_type`、`file_size`、`storage_type`、`storage_key`、`creator`、`created_at`。
- **附件服务** `AttachmentService`：上传、下载、预览、删除、按实例/任务查询列表。
- **REST 接口**（TRD §3.x 扩展，前缀 `/api/v1/attachments`）：
  - `POST /upload`（multipart 上传，关联流程实例/任务/字段）
  - `GET /{id}/download`（`Content-Disposition: attachment`，触发文件下载）
  - `GET /{id}/preview`（内联返回：图片/PDF 用 `inline`，其余建议下载）
  - `DELETE /{id}`（同时清理存储对象）
  - `GET /?processInstanceId=&taskId=` 列表查询

### Out of Scope
- 表单组件层级与附件字段的数据绑定（由 ISSUE-008/009 对接，本期仅交付存储与接口能力）。
- 大文件分片上传、断点续传、缩略图/水印生成。
- 细粒度附件权限（本期仅做登录校验，下载/预览权限继承流程级权限由后续 Issue 细化）。

## 技术约束（以 TRD 为准扩展）
- 表：`wf_attachment`（新增，见下方建表 DDL）。
- 新增依赖：`com.aliyun.oss:aliyun-sdk-oss`（OSS SDK）。
- 配置项（`application.yml`）：
  - `storage.type`：`local` | `oss`
  - `storage.local.base-path`：本地存储根目录（默认 `./attachments`）
  - `storage.oss.endpoint` / `bucket` / `access-key-id` / `access-key-secret` / `region`
- 预览以 `ResponseEntity<Resource>` 返回，正确设置 `Content-Type` 与 `Content-Disposition`。
- 异常复用 `FlowException`（`common/exception`），统一经 `Result`/`@ControllerAdvice` 返回。

```sql
-- TRD §2.1.8 流程附件表（新增）
CREATE TABLE wf_attachment (
    attachment_id      VARCHAR(64)  PRIMARY KEY,
    process_instance_id VARCHAR(64),
    task_id            VARCHAR(64),
    field_key          VARCHAR(64),
    file_name          VARCHAR(255) NOT NULL,
    content_type       VARCHAR(128),
    file_size          BIGINT,
    storage_type       VARCHAR(16)  NOT NULL,   -- local / oss
    storage_key        VARCHAR(512) NOT NULL,   -- 存储后端中的对象 key/路径
    creator            VARCHAR(64),
    created_at         DATETIME     NOT NULL
);
CREATE INDEX idx_attachment_instance ON wf_attachment(process_instance_id);
CREATE INDEX idx_attachment_task ON wf_attachment(task_id);
```

## 接口契约
- REST：`/api/v1/attachments/upload`(POST)、`/{id}/download`(GET)、`/{id}/preview`(GET)、`/{id}`(DELETE)、`/`(GET 列表)。
- Java：
  - `StorageProvider.put(InputStream, key, contentType) / get(key) / delete(key) / generatePresignedUrl(key, expiredSeconds)`
  - `LocalStorageProvider` / `OssStorageProvider` 实现
  - `AttachmentService.upload / download(id) / preview(id) / delete(id) / list(processInstanceId, taskId)`
  - `AttachmentController`（REST 端点）
  - `StorageAutoConfiguration`（按 `storage.type` 装配 Provider）

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 默认 storage.type=local，无需 OSS 凭证即可独立验证上传/下载/预览
```
### 验证用例（映射到本 Issue 验收点）
- [ ] 本地模式上传文件，`wf_attachment` 写入且本地目录生成文件；`storage_type=local`。
- [ ] `GET /{id}/download` 返回 `Content-Disposition: attachment` 与正确 `Content-Type`，文件内容一致。
- [ ] `GET /{id}/preview` 对图片/PDF 返回 `inline`，浏览器内联预览；其余类型提示下载。
- [ ] 切换 `storage.type=oss` 并配置 OSS 参数后，上传/下载/预览走 OSS（预签名 URL 或流式代理），无需改动业务代码。
- [ ] `DELETE /{id}` 同时删除元数据与存储对象；列表查询按实例/任务过滤正确。

### 验证脚本/测试
- `LocalStorageProviderTest`：读写删一致性（默认模式，零外部依赖）。
- `OssStorageProviderTest`：用 OSS endpoint（或 stub/mock client）验证 put/get/delete/预签名 URL。
- `AttachmentServiceTest`：元数据落库与存储联动。
- `AttachmentApiTest`：MockMvc 验证上传/下载/预览/删除/列表接口与响应头。

## 交付物
- 存储抽象（`StorageProvider` 接口 + 本地/OSS 两种实现 + 自动装配配置）
- 附件元数据表 `wf_attachment` 及对应实体/Mapper
- 附件服务与 REST 接口（上传/下载/预览/删除/列表）
- 单元测试 + 集成测试

## 验收门禁（DoD）
- 本地与 OSS 两种存储可通过配置切换且对业务透明；上传/下载/预览/删除/列表全链路可用；图片与 PDF 可在线预览；测试通过。

---

> 注：本 Issue 为 TRD 扩展项，需在 TRD 中补充 §2.1.8 `wf_attachment` 表与 §3.x 附件 API、§5.x 存储抽象设计章节（实现时同步补充）。
