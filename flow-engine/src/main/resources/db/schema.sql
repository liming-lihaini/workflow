-- =====================================================================
-- 自定义流程引擎 数据库初始化脚本 (SQLite)
-- 依据 TRD §2.1 核心表结构，共 19 张表
-- 表前缀: wf_ 流程/表单/数据模型/变量; sys_ 后台管理
-- SQLite 类型映射: BIGINT/INT/TINYINT -> INTEGER; VARCHAR/LONGTEXT/DATETIME -> TEXT
-- =====================================================================

-- 2.1.1 流程定义表
CREATE TABLE IF NOT EXISTS wf_process_definition (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    process_key   TEXT,
    process_name  TEXT,
    version       INTEGER,
    process_json  TEXT,
    category      TEXT,
    process_type  TEXT,
    description   TEXT,
    status        INTEGER,
    deployment_id TEXT,
    create_time   TEXT,
    update_time   TEXT,
    create_by     TEXT
);

-- 2.1.2 流程实例表
CREATE TABLE IF NOT EXISTS wf_process_instance (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    instance_no      TEXT,
    process_key      TEXT,
    process_name     TEXT,
    process_version  INTEGER,
    business_key     TEXT,
    status           INTEGER,
    current_node_id  TEXT,
    start_user       TEXT,
    start_time       TEXT,
    end_time         TEXT,
    duration         INTEGER,
    version          INTEGER DEFAULT 0,
    create_time      TEXT,
    update_time      TEXT
);

-- 2.1.3 任务表
CREATE TABLE IF NOT EXISTS wf_task (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    process_instance_id INTEGER,
    process_key     TEXT,
    node_id         TEXT,
    node_name       TEXT,
    task_type       INTEGER,
    assignee        TEXT,
    candidate_users TEXT,
    claim_time      TEXT,
    complete_time   TEXT,
    task_action   INTEGER DEFAULT 0,
    status          INTEGER,
    counter_sign_group_id INTEGER,
    add_sign_type   TEXT,
    sign_type       TEXT,
    parent_task_id  INTEGER,
    reason          TEXT,
    actual_operator_id TEXT,     -- 实际操作人（代理人代办时记录代理人ID）
    create_time     TEXT,
    update_time     TEXT
);

-- 2.1.4 表单定义表
CREATE TABLE IF NOT EXISTS wf_form_definition (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    form_key    TEXT,
    form_name   TEXT,
    form_json   TEXT,
    category    TEXT,
    model_key   TEXT,
    create_time TEXT,
    update_time TEXT
);

-- 2.1.5 数据模型表
CREATE TABLE IF NOT EXISTS wf_data_model (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    model_key   TEXT,
    model_name  TEXT,
    model_json  TEXT,
    version     INTEGER,
    status      INTEGER,
    create_time TEXT,
    update_time TEXT
);

-- 2.1.6 模型实例表
CREATE TABLE IF NOT EXISTS wf_model_instance (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    model_key          TEXT,
    model_instance_id  TEXT,
    process_instance_id INTEGER,
    data_json          TEXT,
    create_time        TEXT,
    update_time        TEXT
);

-- 2.1.7 流程变量表
CREATE TABLE IF NOT EXISTS wf_variable (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    process_instance_id INTEGER,
    task_id           INTEGER,
    variable_key      TEXT,
    variable_value    TEXT,
    variable_type     TEXT,
    create_time       TEXT
);

-- 2.1.8 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id   INTEGER,
    dept_name   TEXT,
    dept_code   TEXT,
    dept_type   TEXT,
    sort_order  INTEGER,
    leader_id   INTEGER,
    leader_name TEXT,
    phone       TEXT,
    status      INTEGER,
    create_time TEXT,
    update_time TEXT
);

-- 2.1.9 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username       TEXT,
    password       TEXT,
    real_name      TEXT,
    email          TEXT,
    phone          TEXT,
    dept_id        INTEGER,
    post_id        INTEGER,
    security_level INTEGER,
    status         INTEGER,
    create_time    TEXT,
    update_time    TEXT
);

-- 2.1.10 用户兼职表
CREATE TABLE IF NOT EXISTS sys_user_post (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id  INTEGER,
    dept_id  INTEGER,
    post_id  INTEGER,
    is_main  INTEGER
);

-- 2.1.11 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    role_key    TEXT,
    role_name   TEXT,
    role_type   INTEGER,
    parent_id   INTEGER,
    sort_order  INTEGER,
    status      INTEGER,
    create_time TEXT,
    update_time TEXT
);

-- 2.1.12 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id  INTEGER,
    role_id  INTEGER
);

-- 2.1.13 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id     INTEGER,
    perm_name     TEXT,
    perm_key      TEXT,
    perm_type     INTEGER,
    perm_group    TEXT,
    resource_path TEXT,
    sort_order    INTEGER,
    create_time   TEXT
);

-- 2.1.14 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    role_id       INTEGER,
    permission_id INTEGER
);

-- 2.1.15 数据权限表 (TRD 原 typo TININY 已修正为 INTEGER)
CREATE TABLE IF NOT EXISTS sys_data_permission (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    role_id    INTEGER,
    dept_id    INTEGER,
    data_scope INTEGER
);

-- 2.1.16 访问日志表
CREATE TABLE IF NOT EXISTS sys_access_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER,
    username    TEXT,
    ip          TEXT,
    user_agent  TEXT,
    url         TEXT,
    method      TEXT,
    params      TEXT,
    result      INTEGER,
    error_msg   TEXT,
    access_time TEXT
);

-- 2.1.17 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER,
    username       TEXT,
    module         TEXT,
    operation      TEXT,
    method         TEXT,
    params         TEXT,
    result         TEXT,
    before_data    TEXT,
    after_data     TEXT,
    ip             TEXT,
    operation_time TEXT
);

-- 2.1.18 数据字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    dict_name    TEXT,
    dict_code    TEXT,
    dict_type    INTEGER,
    description  TEXT,
    status       INTEGER,
    create_time  TEXT,
    update_time  TEXT
);

-- 2.1.19 数据字典项表
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    dict_type_id INTEGER,
    item_text    TEXT,
    item_value   TEXT,
    sort_order   INTEGER,
    status       INTEGER,
    create_time  TEXT,
    update_time  TEXT
);

-- 会签组表（ISSUE-006）
CREATE TABLE IF NOT EXISTS wf_counter_sign_group (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    process_instance_id INTEGER,
    node_id           TEXT,
    mode              TEXT,
    ratio_threshold   REAL,
    total_count       INTEGER,
    approve_count     INTEGER DEFAULT 0,
    reject_count      INTEGER DEFAULT 0,
    status            INTEGER DEFAULT 0,
    create_time       TEXT,
    update_time       TEXT
);

-- 会签投票表（ISSUE-006）
CREATE TABLE IF NOT EXISTS wf_counter_sign_vote (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id        INTEGER,
    voter           TEXT,
    vote_result     INTEGER,
    comment         TEXT,
    vote_time       TEXT,
    version         INTEGER DEFAULT 0,
    create_time     TEXT
);

-- 加签记录表（ISSUE-007）
CREATE TABLE IF NOT EXISTS wf_add_sign_record (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    original_task_id     INTEGER,
    add_sign_task_id     INTEGER,
    process_instance_id  INTEGER,
    sign_type            TEXT,
    initiator            TEXT,
    add_sign_user        TEXT,
    comment              TEXT,
    status               INTEGER DEFAULT 0,
    create_time          TEXT,
    update_time          TEXT
);

-- 表单数据表（ISSUE-008）
CREATE TABLE IF NOT EXISTS wf_form_data (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    form_key             TEXT NOT NULL,
    process_instance_id  INTEGER,
    task_id              INTEGER,
    data_json            TEXT,
    submit_user          TEXT,
    create_time          TEXT,
    update_time          TEXT
);

-- form_key 唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_form_definition_form_key ON wf_form_definition(form_key);

-- Webhook配置表（ISSUE-012）
CREATE TABLE IF NOT EXISTS wf_webhook (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    webhook_key      TEXT NOT NULL,
    name             TEXT,
    url              TEXT NOT NULL,
    method           TEXT DEFAULT 'POST',
    headers          TEXT,
    payload_template TEXT,
    timeout          INTEGER DEFAULT 5000,
    retry_count      INTEGER DEFAULT 3,
    trigger_events   TEXT,
    process_key      TEXT,
    node_id          TEXT,
    status           INTEGER DEFAULT 1,
    create_time      TEXT,
    update_time      TEXT
);

-- Webhook回调日志表（ISSUE-012）
CREATE TABLE IF NOT EXISTS wf_webhook_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    webhook_id      INTEGER,
    webhook_key     TEXT,
    event_type      TEXT,
    process_instance_id INTEGER,
    request_url     TEXT,
    request_method  TEXT,
    request_body    TEXT,
    response_status INTEGER,
    response_body   TEXT,
    status          INTEGER DEFAULT 0,
    retry_count     INTEGER DEFAULT 0,
    error_message   TEXT,
    trigger_time    TEXT,
    complete_time   TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_webhook_key ON wf_webhook(webhook_key);

-- 全局委托表（替代旧的任务级委托表）
DROP TABLE IF EXISTS wf_delegation;
CREATE TABLE IF NOT EXISTS wf_delegation (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    delegator_id  TEXT NOT NULL,   -- 委托人用户ID
    delegate_id   TEXT NOT NULL,   -- 代理人用户ID
    start_time    TEXT,            -- 委托开始时间
    end_time      TEXT,            -- 委托结束时间（为空则永久）
    reason        TEXT,            -- 委托说明
    status        INTEGER DEFAULT 0, -- 0-生效中 2-已取消 3-已过期
    create_time   TEXT,
    update_time   TEXT
);

-- 个人API Token表（支持APItoken方式鉴权）
CREATE TABLE IF NOT EXISTS sys_api_token (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    token_name      TEXT,
    token_value     TEXT,
    expire_time     TEXT,            -- 为空则永久有效
    last_used_time  TEXT,
    create_time     TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_api_token_value ON sys_api_token(token_value);

-- =====================================================================
-- 迁移脚本：为旧表添加缺失列（已存在时会报错但 continue-on-error=true 会跳过）
-- =====================================================================
ALTER TABLE wf_task ADD COLUMN reason TEXT;
ALTER TABLE wf_task ADD COLUMN actual_operator_id TEXT;
ALTER TABLE t_sample ADD COLUMN sampler TEXT;
ALTER TABLE t_sample ADD COLUMN sample_time TEXT;
ALTER TABLE t_retain ADD COLUMN retain_no TEXT;
ALTER TABLE t_retain ADD COLUMN category TEXT;
ALTER TABLE t_retain ADD COLUMN retain_location TEXT;
ALTER TABLE t_retain ADD COLUMN dispose_reason TEXT;
ALTER TABLE t_retain ADD COLUMN dispose_method TEXT;
ALTER TABLE t_retain ADD COLUMN dispose_date TEXT;
ALTER TABLE t_retain ADD COLUMN dispose_time TEXT;
ALTER TABLE t_retain ADD COLUMN process_instance_id INTEGER;

-- =====================================================================
-- 环境监测 LIMS 基础数据（ISSUE-022）—— 独立业务域，表前缀 t_
-- 与既有 wf_/sys_ 流程引擎表互不耦合
-- =====================================================================

-- 客户档案（TRD 5.1）
CREATE TABLE IF NOT EXISTS t_customer (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    cust_no       TEXT,
    cust_name     TEXT NOT NULL,
    credit_code   TEXT,
    contact       TEXT,
    tel           TEXT,
    invoice_title TEXT,
    tax_no        TEXT,
    qual_file_id  INTEGER,
    city          TEXT,               -- 所在城市
    address       TEXT,               -- 办公地址
    status        INTEGER DEFAULT 1,   -- 1-启用 0-停用
    create_time   TEXT,
    update_time   TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_unique ON t_customer(cust_name, credit_code);

-- 监测点位（TRD 5.1）
CREATE TABLE IF NOT EXISTS t_monitor_point (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    cust_id          INTEGER,
    entrust_id       INTEGER,
    point_no         TEXT,
    point_name       TEXT NOT NULL,
    lng              REAL,
    lat              REAL,
    point_type       TEXT,
    point_type_name  TEXT,           -- 点位类型名称（字典 value）
    condition        TEXT,
    factors          TEXT,           -- 监测因子（检测项目，多个用逗号分隔）
    standard_code    TEXT,           -- 执行标准编号（多个用逗号分隔）
    standard_name    TEXT,           -- 执行标准全称（多个用逗号分隔）
    freq             TEXT,           -- 监测频次
    history_over_flag INTEGER DEFAULT 0,
    create_time      TEXT,
    update_time      TEXT
);

-- 兼容旧库：若 t_monitor_point 已存在但缺少新增列，则自动补列（SQLite 不支持 IF NOT EXISTS 的 ALTER，用异常忽略方式）
ALTER TABLE t_monitor_point ADD COLUMN factors TEXT;
ALTER TABLE t_monitor_point ADD COLUMN standard_code TEXT;
ALTER TABLE t_monitor_point ADD COLUMN standard_name TEXT;
ALTER TABLE t_monitor_point ADD COLUMN freq TEXT;
ALTER TABLE t_monitor_point ADD COLUMN point_type_name TEXT;

-- 委托单主数据框架（TRD 5.1，完整状态机见 ISSUE-023）
CREATE TABLE IF NOT EXISTS t_entrust (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    entrust_no   TEXT,
    cust_id      INTEGER,
    entrust_name TEXT,
    source       TEXT,
    status       TEXT DEFAULT '草稿',  -- 草稿/待技术确认/已确认/已退回
    description   TEXT,                -- 委托说明（富文本）
    sample_freq   TEXT,                -- 委托级采集频率（字典 moni_sample_freq 编码）
    sample_freq_name TEXT,             -- 采集频率名称（字典 value）
    submit_by    TEXT,
    source_name  TEXT,                 -- 来源名称（字典 value）
    create_by    TEXT,                 -- 创建人-工号（username）
    create_name  TEXT,                 -- 创建人-姓名（realName）
    update_by    TEXT,                 -- 更新人-工号（username）
    update_name  TEXT,                 -- 更新人-姓名（realName）
    create_time  TEXT,
    update_time  TEXT
);

-- ===== ISSUE-023 监测任务管理（TRD 5.1 委托 / 5.2 采样调度） =====
-- 委托明细（监测项目/执行标准/频次/样品要求/限值）
CREATE TABLE IF NOT EXISTS t_entrust_detail (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    entrust_id   INTEGER,
    item         TEXT,
    standard_id  INTEGER,
    freq         TEXT,
    sample_req   TEXT,
    limit_val    TEXT,
    create_time  TEXT,
    update_time  TEXT
);
-- 技术确认记录（BR-023-01）
CREATE TABLE IF NOT EXISTS t_entrust_review (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    entrust_id   INTEGER,
    reviewer_id  INTEGER,
    opinion      TEXT,
    result       TEXT,   -- PASS-通过 / REJECT-退回
    review_at    TEXT
);
-- 采样订单（BR-023-02 拆单生成，状态机核心）
CREATE TABLE IF NOT EXISTS t_sampling_order (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no     TEXT,
    entrust_id   INTEGER,
    point_id     INTEGER,
    plan_date    TEXT,
    sampler_lead TEXT,
    status       TEXT DEFAULT '待派单',  -- 待派单/已派单/采样执行中/样品送检/实验室检测中/报告编制/归档完成
    create_time  TEXT,
    update_time  TEXT
);
-- 调度派单主表（TRD 5.2）
CREATE TABLE IF NOT EXISTS t_dispatch (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id     INTEGER,
    status       TEXT DEFAULT '待派单',  -- 待派单/已派单/采样执行中/样品送检
    dispatch_time TEXT,
    plan_start   TEXT,
    plan_end     TEXT,
    vehicle_id   INTEGER,
    note         TEXT,
    create_time  TEXT,
    update_time  TEXT
);
CREATE TABLE IF NOT EXISTS t_dispatch_member (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    dispatch_id  INTEGER,
    emp_id       INTEGER,
    role         TEXT    -- LEAD-负责 / MEMBER-组员
);
CREATE TABLE IF NOT EXISTS t_dispatch_device (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    dispatch_id  INTEGER,
    instrument_id INTEGER
);
-- 采样人员（ISSUE-023 派单资源；030 人员体系基础）
CREATE TABLE IF NOT EXISTS t_employee (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT,
    phone        TEXT,
    qual_file_id INTEGER,
    qual_due     TEXT,   -- 资质有效期（闸门校验）
    status       INTEGER DEFAULT 1,
    create_time  TEXT,
    update_time  TEXT
);
-- 采样设备/仪器（校准有效期闸门）
CREATE TABLE IF NOT EXISTS t_instrument (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    code           TEXT,   -- 仪器编号(YQ前缀)
    name           TEXT,
    model          TEXT,
    manufacturer   TEXT,   -- 生产厂商
    purchase_date  TEXT,   -- 购置日期
    calib_due      TEXT,   -- 校准到期日（物资闸门：过期强制停用）
    last_calib_date TEXT,  -- 上次校准日期
    cert_no        TEXT,   -- 校准证书编号
    status         TEXT DEFAULT '在用',  -- 在用/临期/停用/维修/报废
    remark         TEXT,
    create_time    TEXT,
    update_time    TEXT
);

-- 组织机构/部门（TRD 5.10）
CREATE TABLE IF NOT EXISTS t_department (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    dept_no     TEXT,
    dept_name   TEXT NOT NULL,
    parent_id   INTEGER DEFAULT 0,
    leader_id   INTEGER,
    leader_name TEXT,
    status      INTEGER DEFAULT 1,
    create_time TEXT,
    update_time TEXT
);

-- 集成配置（TRD 5.10，密钥 AES 密文）
CREATE TABLE IF NOT EXISTS t_integration_cfg (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    cfg_key     TEXT NOT NULL,
    cfg_value   TEXT,
    encrypt_flag INTEGER DEFAULT 0,
    remark      TEXT,
    create_time TEXT,
    update_time TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_integration_cfg_key ON t_integration_cfg(cfg_key);

-- 文件元信息（TRD 4.4 共享实体，WORM 受控）
CREATE TABLE IF NOT EXISTS t_file_meta (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    biz_type  TEXT,
    biz_id    INTEGER,
    file_name TEXT,
    file_path TEXT,
    hash      TEXT,
    size      INTEGER,
    upload_by TEXT,
    create_time TEXT
);

-- 采样车辆（TRD 4.4 共享实体）
CREATE TABLE IF NOT EXISTS t_vehicle (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    plate_no TEXT,
    model    TEXT,
    status   INTEGER DEFAULT 1,   -- 1-可用 2-占用 3-维修保养中（字典 moni_vehicle_status）
    remark   TEXT,
    create_time TEXT,
    update_time TEXT
);

-- 车辆维修保养记录（ISSUE-036）
CREATE TABLE IF NOT EXISTS t_vehicle_maintenance (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id  INTEGER,
    maint_type  TEXT,             -- 操作类型（字典 moni_vehicle_maint_type 的 item_value）
    start_date  TEXT,             -- 保养开始日期
    end_date    TEXT,             -- 保养结束日期
    remark      TEXT,             -- 备注说明
    create_time TEXT,
    update_time TEXT
);

-- 预警（TRD 4.4 共享实体）
CREATE TABLE IF NOT EXISTS t_alert (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    alert_type TEXT,
    biz_id     INTEGER,
    level      TEXT,
    msg        TEXT,
    status     INTEGER DEFAULT 0,   -- 0-待处理 1-已处理
    create_time TEXT
);

-- 站内信（TRD 4.4 共享实体）
CREATE TABLE IF NOT EXISTS t_message (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    to_user   TEXT,
    title     TEXT,
    content   TEXT,
    read_flag INTEGER DEFAULT 0,
    create_time TEXT
);

-- 设备校准历史记录（TRD 5.5.5 校准登记台账）
CREATE TABLE IF NOT EXISTS t_instrument_calib (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    instrument_id INTEGER,
    calib_date    TEXT,   -- 校准日期
    calib_due     TEXT,   -- 下次校准到期日
    cert_no       TEXT,   -- 校准证书编号
    create_time   TEXT
);

-- ============ ISSUE-024 采样与样品管理 ============

CREATE TABLE IF NOT EXISTS t_sampling_record (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id      INTEGER,      -- 采样订单ID
    dispatch_id   INTEGER,      -- 派单ID
    point_id      INTEGER,      -- 监测点位ID
    sampler       TEXT,         -- 采样人
    sample_time   TEXT,         -- 采样时间
    weather       TEXT,         -- 天气
    status        TEXT,         -- 采样中/采样完成/已收样
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

CREATE TABLE IF NOT EXISTS t_photo (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    biz_type      TEXT,         -- sampling_record / sample
    biz_id        INTEGER,      -- 关联业务ID
    url           TEXT,         -- 图片地址
    create_time   TEXT
);

CREATE TABLE IF NOT EXISTS t_sample (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode       TEXT,         -- 样品条码（YP前缀）
    sampling_id   INTEGER,      -- 采样记录ID
    order_id      INTEGER,      -- 采样订单ID
    point_id      INTEGER,      -- 监测点位ID
    name          TEXT,         -- 样品名称
    type          TEXT,         -- 样品类型（水样/气样/土壤等）
    source        TEXT,         -- 样品来源（点位）
    amount        TEXT,         -- 数量/规格
    container     TEXT,         -- 容器
    preserve      TEXT,         -- 保存条件/固定剂
    check_items   TEXT,         -- 收样检查单（多选，逗号分隔数据字典值），来源 sample_receive_check
    status        TEXT,         -- 待收样/已收样/异常拒收/留样中/实验室监测中/检测数据复核中/已完成/检测异常/已处置
    disposal_type    TEXT,       -- 异常处置类型（数据字典 moni_disposal_type）
    disposal_method  TEXT,       -- 异常处置方式（数据字典 moni_disposal_method）
    disposal_desc    TEXT,       -- 异常处置说明（富文本 HTML）
    disposal_by      TEXT,       -- 异常处置人
    disposal_time    TEXT,       -- 异常处置时间
    receive_time  TEXT,         -- 收样时间
    receive_by    TEXT,         -- 收样人
    retain_flag   INTEGER,      -- 是否留样（0/1）
    retain_days   INTEGER,      -- 留样天数
    retain_until  TEXT,         -- 留样到期日
    dispatch_time TEXT,         -- 送检/检测下发时间
    sampler       TEXT,         -- 采样人（手动收集场景冗余存储）
    sample_time   TEXT,         -- 采样时间（手动收集场景冗余存储）
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

CREATE TABLE IF NOT EXISTS t_sample_qc_binding (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    sample_id     INTEGER,      -- 样品ID
    sample_no     TEXT,         -- 样品编号
    qc_type       TEXT,         -- 全程序空白/现场空白/平行样/加标回收/密码样
    remark        TEXT
);

CREATE TABLE IF NOT EXISTS t_sample_log (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    sample_id     INTEGER,      -- 样品ID
    action        TEXT,         -- 收样/留样/送检/处置
    operator      TEXT,         -- 操作人
    detail        TEXT,         -- 操作详情
    create_time   TEXT
);

CREATE TABLE IF NOT EXISTS t_retain (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    retain_no     TEXT,         -- 留样编号（LY + yyyyMMdd + 三位序号）
    sample_id     INTEGER,      -- 样品ID
    barcode       TEXT,         -- 样品条码
    name          TEXT,         -- 样品名称
    category      TEXT,         -- 监测类别（冗余自 t_sample.category）
    retain_location TEXT,       -- 库位（留样存放位置）
    dispose_reason   TEXT,      -- 销毁原因
    dispose_method   TEXT,      -- 销毁方式
    dispose_date     TEXT,      -- 预计销毁日期
    dispose_time     TEXT,      -- 实际销毁时间
    process_instance_id INTEGER, -- 关联流程实例ID
    name          TEXT,         -- 样品名称
    point_id      INTEGER,      -- 点位ID
    retain_by     TEXT,         -- 留样人
    retain_time   TEXT,         -- 留样时间
    retain_days   INTEGER,      -- 留样天数
    retain_until  TEXT,         -- 留样到期日
    dispose_time  TEXT,         -- 处置时间
    dispose_by    TEXT,         -- 处置人
    status        TEXT,         -- 留样中/已处置
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- ============ ISSUE-025 检测数据录入与复核 ============

-- 检测任务（每个已收样样品生成一个检测任务，录入员填报监测项目结果）
CREATE TABLE IF NOT EXISTS t_detection_task (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    task_no       TEXT,         -- 任务编号（DT前缀）
    sample_id     INTEGER,      -- 样品ID
    barcode       TEXT,         -- 样品条码
    sample_name   TEXT,         -- 样品名称
    point_id      INTEGER,      -- 监测点位ID
    monitor_items TEXT,         -- 监测项目（逗号分隔，如 pH,COD,氨氮）
    entry_by      TEXT,         -- 录入员
    entry_time    TEXT,         -- 录入时间
    status        TEXT,         -- 待录入/录入中/已提交/已复核/已退回
    review_by     TEXT,         -- 复核人
    review_time   TEXT,         -- 复核时间
    review_opinion TEXT,        -- 复核意见
    env_temp      TEXT,         -- 检测环境温度
    env_humidity  TEXT,         -- 检测环境湿度
    conclusion    TEXT,         -- 综合检验结论：pending/ok/ng/abnormal
    remark        TEXT,
    attachments   TEXT,          -- 检测录入附件（JSON 数组：[{name,path}]）
    create_time   TEXT,
    update_time   TEXT
);

-- 兼容已存在库：补充新增列
ALTER TABLE t_detection_task ADD COLUMN env_temp TEXT;
ALTER TABLE t_detection_task ADD COLUMN env_humidity TEXT;
ALTER TABLE t_detection_task ADD COLUMN conclusion TEXT;
ALTER TABLE t_detection_task ADD COLUMN attachments TEXT;
ALTER TABLE t_sample_param_config ADD COLUMN unit TEXT;
ALTER TABLE t_sample_param_config ADD COLUMN inner_limit TEXT;

-- 检测结果明细（每个监测项目一条记录）
CREATE TABLE IF NOT EXISTS t_detection_result (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id       INTEGER,      -- 检测任务ID
    sample_id     INTEGER,      -- 样品ID
    monitor_item  TEXT,         -- 监测项目（pH/COD/氨氮等）
    value         TEXT,         -- 检测值
    unit          TEXT,         -- 单位
    method        TEXT,         -- 检测方法/标准
    limit_value   TEXT,         -- 标准限值
    conclusion    TEXT,         -- 达标/超标
    create_time   TEXT,
    update_time   TEXT
);

-- 复核记录（每次复核留痕）
CREATE TABLE IF NOT EXISTS t_detection_review (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id       INTEGER,      -- 检测任务ID
    sample_id     INTEGER,      -- 样品ID
    barcode       TEXT,         -- 样品条码
    reviewer      TEXT,         -- 复核人
    decision      TEXT,         -- 通过/退回
    opinion       TEXT,         -- 复核意见
    create_time   TEXT
);

-- ============ ISSUE-026 质量控制（物资 + 质控计划 + 能力验证） ============

-- 标准物质（5.5 效期闸门）
CREATE TABLE IF NOT EXISTS t_standard_material (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT,         -- 标物名称
    lot_no        TEXT,         -- 批号
    spec          TEXT,         -- 规格/浓度
    expire_date   TEXT,         -- 效期
    stock         INTEGER,      -- 库存
    status        TEXT,         -- 在库/临期/过期
    cert_no       TEXT,         -- 证书编号
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- 耗材（5.5 效期管理）
CREATE TABLE IF NOT EXISTS t_consumable (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT,         -- 耗材名称
    spec          TEXT,         -- 规格
    qty           INTEGER,      -- 数量
    expire_date   TEXT,         -- 效期
    status        TEXT,         -- 在库/临期/过期
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- 危化品台账（5.5 审批状态机：在库→待审批→已领用/已报废）
CREATE TABLE IF NOT EXISTS t_hazardous_ledger (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT,         -- 危化品名称
    cas_no        TEXT,         -- CAS号
    category      TEXT,         -- 类别（易燃/腐蚀/有毒等）
    qty           TEXT,         -- 数量
    unit          TEXT,         -- 单位
    status        TEXT,         -- 在库/待审批/已领用/已报废
    apply_by      TEXT,         -- 申请人
    approve_by    TEXT,         -- 审批人
    apply_reason  TEXT,         -- 申请用途
    approve_opinion TEXT,       -- 审批意见
    apply_time    TEXT,
    approve_time  TEXT,
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- 质控计划（5.12 状态机：草稿→审批中→执行中→已完成）
CREATE TABLE IF NOT EXISTS t_qc_plan (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_no       TEXT,         -- 计划编号（QC前缀）
    title         TEXT,         -- 计划名称
    year          INTEGER,      -- 年度
    quarter       TEXT,         -- 季度（Q1-Q4）/ 专项
    type          TEXT,         -- 年度/季度/专项
    responsible_id TEXT,        -- 责任人
    status        TEXT,         -- 草稿/审批中/执行中/已完成
    approved_by   TEXT,         -- 审批人
    approved_at   TEXT,         -- 审批时间
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- 监控活动（5.12 空白/平行/加标回收/留样复测，联动025）
CREATE TABLE IF NOT EXISTS t_qc_activity (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id       INTEGER,      -- 所属质控计划
    qc_type       TEXT,         -- 空白/平行/加标回收/留样复测
    item          TEXT,         -- 监测项目
    standard_id   INTEGER,      -- 关联标准物质
    batch_id      INTEGER,      -- 关联检测批次（可空，联动025）
    result        TEXT,         -- 检测结果
    pass_flag     TEXT,         -- 合格/不合格
    operator_id   TEXT,         -- 操作人
    act_date      TEXT,         -- 活动日期
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- 能力验证（5.12 G6）
CREATE TABLE IF NOT EXISTS t_proficiency_test (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id       INTEGER,      -- 所属计划（可空）
    org           TEXT,         -- 外部机构
    item          TEXT,         -- 项目
    standard_id   INTEGER,      -- 关联标准物质
    result        TEXT,         -- 结果
    conclusion    TEXT,         -- 合格/不合格
    cert_file     TEXT,         -- 证书文件
    employee_ids  TEXT,         -- 参与人员(JSON)
    test_date     TEXT,         -- 验证日期
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- 实验室间比对（5.12 G6）
CREATE TABLE IF NOT EXISTS t_interlab_compare (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id       INTEGER,      -- 所属计划（可空）
    partner_lab   TEXT,         -- 合作实验室
    item          TEXT,         -- 项目
    standard_id   INTEGER,      -- 关联标准物质
    our_value     TEXT,         -- 我方值
    ref_value     TEXT,         -- 参考值
    deviation     TEXT,         -- 偏差
    conclusion    TEXT,         -- 合格/不合格
    compare_date  TEXT,         -- 比对日期
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- 重复性试验（5.12 G6）
CREATE TABLE IF NOT EXISTS t_repeat_test (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id       INTEGER,      -- 所属计划（可空）
    item          TEXT,         -- 项目
    standard_id   INTEGER,      -- 关联标准物质
    first_value   TEXT,         -- 首次值
    repeat_value  TEXT,         -- 重复值
    deviation     TEXT,         -- 偏差
    conclusion    TEXT,         -- 合格/不合格
    operator_id   TEXT,         -- 操作人
    test_date     TEXT,         -- 试验日期
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- ============ ISSUE-027 报告生成与审核（5.11） ============

-- 报告模板（季度/月度/委托）
CREATE TABLE IF NOT EXISTS t_report_template (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    tpl_no        TEXT,         -- 模板编号（RPT前缀）
    name          TEXT,         -- 模板名称
    type          TEXT,         -- 季度/月度/委托
    content       TEXT,         -- 模板内容（含占位说明）
    enabled       TEXT,         -- 1启用 0停用
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

-- 报告头（状态机：待生成→待审核→已发布/已退回）
CREATE TABLE IF NOT EXISTS t_report (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    report_no     TEXT,         -- 报告编号（RP前缀）
    title         TEXT,         -- 报告标题
    tpl_id        INTEGER,      -- 模板ID
    tpl_type      TEXT,         -- 模板类型（冗余）
    client        TEXT,         -- 委托单位
    period        TEXT,         -- 报告周期（如 2026-Q2）
    task_ids      TEXT,         -- 关联检测任务ID(JSON数组)
    item_count    INTEGER,      -- 明细项数
    exceed_count  INTEGER,      -- 超标项数
    status        TEXT,         -- 待审核/已发布/已退回
    anti_fake_code TEXT,        -- 防伪码（发布后生成）
    generator     TEXT,         -- 生成人
    publish_time  TEXT,         -- 发布时间
    create_time   TEXT,
    update_time   TEXT
);

-- 报告明细（关联检测任务结果，含结论/是否超标）
CREATE TABLE IF NOT EXISTS t_report_item (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id     INTEGER,      -- 报告ID
    task_id       INTEGER,      -- 检测任务ID
    item          TEXT,         -- 监测项目
    sample_code   TEXT,         -- 样品编号
    result        TEXT,         -- 检测结果
    unit          TEXT,         -- 单位
    standard_limit TEXT,        -- 标准限值
    conclusion    TEXT,         -- 达标/超标
    create_time   TEXT
);

-- 报告审核记录（5.11）
CREATE TABLE IF NOT EXISTS t_report_audit (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id     INTEGER,      -- 报告ID
    auditor       TEXT,         -- 审核人
    decision      TEXT,         -- 通过/退回
    opinion       TEXT,         -- 审核意见
    create_time   TEXT
);

-- ============ TRD 5.1 采样参数配置管理 ============

-- 采样参数配置主表（检测类别 + 检测项目 + 执行标准 + 限值 + 备注）
CREATE TABLE IF NOT EXISTS t_sample_param_config (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    type          TEXT,         -- 检测类别（废气/废水/土壤...）
    item          TEXT,         -- 检测项目（颗粒物/CODcr...）
    standard      TEXT,         -- 执行标准编号
    limit_value   TEXT,         -- 标准限值 / 管控要求
    unit          TEXT,         -- 检测结果单位（mg/L、mg/m³ 等）
    inner_limit   TEXT,         -- 企业内控限制（严于国标的企业内部管控要求）
    remark        TEXT,         -- 采样备注说明
    create_time   TEXT,
    update_time   TEXT
);

-- 采样参数配置明细（现场结构化必填采样参数，与主表一对多）
CREATE TABLE IF NOT EXISTS t_sample_param_item (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    config_id     INTEGER,      -- 关联主表 t_sample_param_config.id
    code          TEXT,         -- 参数编码（如 flue_area）
    name          TEXT,         -- 参数名称（如 烟道截面积）
    param_type    TEXT,         -- number/text/select/bool/datetime
    unit          TEXT,         -- 单位（如 m²）
    required      INTEGER DEFAULT 1, -- 1 必填 / 0 选填
    enum_text     TEXT,         -- 下拉选项，逗号分隔（仅 select 类型）
    tip           TEXT,         -- 提示备注
    sort_no       INTEGER       -- 排序
);

-- 兼容旧库：为 t_sample 补收样检查单列（SQLite 不支持 IF NOT EXISTS 的 ALTER，异常忽略即可）
ALTER TABLE t_sample ADD COLUMN check_items TEXT;

