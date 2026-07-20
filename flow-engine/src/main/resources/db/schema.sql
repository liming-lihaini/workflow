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
    status        INTEGER,
    deployment_id TEXT,
    create_time   TEXT,
    update_time   TEXT,
    create_by     TEXT
);

-- 2.1.2 流程实例表
CREATE TABLE IF NOT EXISTS wf_process_instance (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
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
    parent_task_id  INTEGER,
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
