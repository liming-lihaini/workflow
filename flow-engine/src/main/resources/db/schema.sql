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
    condition        TEXT,
    history_over_flag INTEGER DEFAULT 0,
    create_time      TEXT,
    update_time      TEXT
);

-- 委托单主数据框架（TRD 5.1，完整状态机见 ISSUE-023）
CREATE TABLE IF NOT EXISTS t_entrust (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    entrust_no   TEXT,
    cust_id      INTEGER,
    entrust_name TEXT,
    source       TEXT,
    status       TEXT DEFAULT '草稿',  -- 草稿/待技术确认/已确认/已退回
    description   TEXT,                -- 委托说明（富文本）
    submit_by    TEXT,
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
    status   INTEGER DEFAULT 1,   -- 1-可用 2-维修 0-停用（字典 moni_vehicle_status）
    remark   TEXT,
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
    status        TEXT,         -- 待收样/已收样/留样中/已处置
    receive_time  TEXT,         -- 收样时间
    receive_by    TEXT,         -- 收样人
    retain_flag   INTEGER,      -- 是否留样（0/1）
    retain_days   INTEGER,      -- 留样天数
    retain_until  TEXT,         -- 留样到期日
    dispatch_time TEXT,         -- 送检/检测下发时间
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
    sample_id     INTEGER,      -- 样品ID
    barcode       TEXT,         -- 样品条码
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
    remark        TEXT,
    create_time   TEXT,
    update_time   TEXT
);

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

