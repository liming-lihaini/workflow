-- =====================================================================
-- flow_engine 数据库 MySQL 8.0 全量建表脚本
-- 由 SQLite 库 flow_engine.db 自动迁移生成
-- 字符集: utf8mb4 / 排序: utf8mb4_general_ci
-- 共 78 张表, 0 个视图
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `dm_ddl_test_model_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `main_id` BIGINT,
    `item_name` VARCHAR(512) NOT NULL DEFAULT '',
    `qty` VARCHAR(512),
    `subtotal` VARCHAR(512),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='dm_ddl_test_model_items';

CREATE TABLE IF NOT EXISTS `dm_ddl_test_model_main` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(512) NOT NULL DEFAULT '',
    `amount` VARCHAR(512),
    `apply_date` DATETIME(6),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='dm_ddl_test_model_main';

CREATE TABLE IF NOT EXISTS `sys_access_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT,
    `username` VARCHAR(512),
    `ip` VARCHAR(512),
    `user_agent` VARCHAR(512),
    `url` VARCHAR(512),
    `method` LONGTEXT,
    `params` LONGTEXT,
    `result` LONGTEXT,
    `error_msg` LONGTEXT,
    `access_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_access_log';

CREATE TABLE IF NOT EXISTS `sys_api_token` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL DEFAULT 0,
    `token_name` VARCHAR(512),
    `token_value` LONGTEXT,
    `expire_time` DATETIME(6),
    `last_used_time` DATETIME(6),
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_api_token';

CREATE TABLE IF NOT EXISTS `sys_data_permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_id` BIGINT,
    `dept_id` BIGINT,
    `data_scope` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_data_permission';

CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `parent_id` BIGINT,
    `dept_name` VARCHAR(512),
    `dept_code` VARCHAR(512),
    `dept_type` VARCHAR(512),
    `sort_order` BIGINT,
    `leader_id` BIGINT,
    `leader_name` VARCHAR(512),
    `phone` VARCHAR(512),
    `status` BIGINT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_dept';

CREATE TABLE IF NOT EXISTS `sys_dict_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `dict_type_id` BIGINT,
    `item_text` VARCHAR(512),
    `item_value` LONGTEXT,
    `sort_order` BIGINT,
    `status` BIGINT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_dict_item';

CREATE TABLE IF NOT EXISTS `sys_dict_type` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `dict_name` VARCHAR(512),
    `dict_code` VARCHAR(512),
    `dict_type` BIGINT,
    `description` LONGTEXT,
    `status` BIGINT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_dict_type';

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT,
    `username` VARCHAR(512),
    `module` VARCHAR(512),
    `operation` VARCHAR(512),
    `method` LONGTEXT,
    `params` LONGTEXT,
    `result` LONGTEXT,
    `before_data` LONGTEXT,
    `after_data` LONGTEXT,
    `ip` VARCHAR(512),
    `operation_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_operation_log';

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `parent_id` BIGINT,
    `perm_name` VARCHAR(512),
    `perm_key` VARCHAR(512),
    `perm_type` BIGINT,
    `perm_group` VARCHAR(512),
    `resource_path` VARCHAR(512),
    `sort_order` BIGINT,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_permission';

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_key` VARCHAR(512),
    `role_name` VARCHAR(512),
    `role_type` BIGINT,
    `parent_id` BIGINT,
    `sort_order` BIGINT,
    `status` BIGINT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_role';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role_id` BIGINT,
    `permission_id` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_role_permission';

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(512),
    `password` VARCHAR(512),
    `real_name` VARCHAR(512),
    `email` VARCHAR(512),
    `phone` VARCHAR(512),
    `dept_id` BIGINT,
    `post_id` BIGINT,
    `security_level` BIGINT,
    `status` BIGINT,
    `gender` VARCHAR(16),
    `birth_date` VARCHAR(32),
    `avatar` VARCHAR(512),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_user';

CREATE TABLE IF NOT EXISTS `sys_user_post` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT,
    `dept_id` BIGINT,
    `post_id` BIGINT,
    `is_main` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_user_post';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT,
    `role_id` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_user_role';

CREATE TABLE IF NOT EXISTS `sys_user_qualification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT,
    `qual_name` VARCHAR(512),
    `cert_no` VARCHAR(512),
    `issuer` VARCHAR(512),
    `expire_date` VARCHAR(512),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sys_user_qualification';

CREATE TABLE IF NOT EXISTS `t_alert` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `alert_type` VARCHAR(512),
    `biz_id` BIGINT,
    `level` VARCHAR(512),
    `msg` LONGTEXT,
    `status` BIGINT DEFAULT 0,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_alert';

CREATE TABLE IF NOT EXISTS `t_consumable` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(512),
    `spec` VARCHAR(512),
    `qty` BIGINT,
    `expire_date` DATETIME(6),
    `status` VARCHAR(512),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_consumable';

CREATE TABLE IF NOT EXISTS `t_customer` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `cust_no` VARCHAR(512),
    `cust_name` VARCHAR(512) NOT NULL DEFAULT '',
    `credit_code` VARCHAR(512),
    `contact` VARCHAR(512),
    `tel` VARCHAR(512),
    `invoice_title` VARCHAR(512),
    `tax_no` VARCHAR(512),
    `qual_file_id` BIGINT,
    `status` BIGINT DEFAULT 1,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `city` VARCHAR(512),
    `address` LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_customer';

CREATE TABLE IF NOT EXISTS `t_data_dict` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `dict_type` VARCHAR(512) NOT NULL DEFAULT '',
    `dict_label` VARCHAR(512),
    `dict_value` LONGTEXT,
    `sort` BIGINT DEFAULT 0,
    `status` BIGINT DEFAULT 1,
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_data_dict';

CREATE TABLE IF NOT EXISTS `t_department` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `dept_no` VARCHAR(512),
    `dept_name` VARCHAR(512) NOT NULL DEFAULT '',
    `parent_id` BIGINT DEFAULT 0,
    `leader_id` BIGINT,
    `leader_name` VARCHAR(512),
    `status` BIGINT DEFAULT 1,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_department';

CREATE TABLE IF NOT EXISTS `t_detection_result` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` BIGINT,
    `sample_id` BIGINT,
    `monitor_item` VARCHAR(512),
    `value` LONGTEXT,
    `unit` VARCHAR(512),
    `method` LONGTEXT,
    `limit_value` LONGTEXT,
    `conclusion` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_detection_result';

CREATE TABLE IF NOT EXISTS `t_detection_review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` BIGINT,
    `sample_id` BIGINT,
    `barcode` VARCHAR(512),
    `reviewer` VARCHAR(512),
    `decision` VARCHAR(512),
    `opinion` LONGTEXT,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_detection_review';

CREATE TABLE IF NOT EXISTS `t_detection_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_no` VARCHAR(512),
    `sample_id` BIGINT,
    `barcode` VARCHAR(512),
    `sample_name` VARCHAR(512),
    `point_id` BIGINT,
    `monitor_items` VARCHAR(512),
    `entry_by` VARCHAR(512),
    `entry_time` DATETIME(6),
    `status` VARCHAR(512),
    `review_by` VARCHAR(512),
    `review_time` DATETIME(6),
    `review_opinion` LONGTEXT,
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `env_temp` VARCHAR(512),
    `env_humidity` VARCHAR(512),
    `conclusion` LONGTEXT,
    `attachments` LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_detection_task';

CREATE TABLE IF NOT EXISTS `t_dispatch` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id` BIGINT,
    `status` VARCHAR(512) DEFAULT '''待派单''',
    `dispatch_time` DATETIME(6),
    `plan_start` VARCHAR(512),
    `plan_end` VARCHAR(512),
    `vehicle_id` BIGINT,
    `note` VARCHAR(512),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_dispatch';

CREATE TABLE IF NOT EXISTS `t_dispatch_device` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `dispatch_id` BIGINT,
    `instrument_id` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_dispatch_device';

CREATE TABLE IF NOT EXISTS `t_dispatch_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `dispatch_id` BIGINT,
    `emp_id` BIGINT,
    `role` VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_dispatch_member';

CREATE TABLE IF NOT EXISTS `t_employee` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(512),
    `phone` VARCHAR(512),
    `qual_file_id` BIGINT,
    `qual_due` VARCHAR(512),
    `status` BIGINT DEFAULT 1,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_employee';

CREATE TABLE IF NOT EXISTS `t_entrust` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `entrust_no` VARCHAR(512),
    `cust_id` BIGINT,
    `entrust_name` VARCHAR(512),
    `source` VARCHAR(512),
    `status` VARCHAR(512) DEFAULT '''草稿''',
    `submit_by` VARCHAR(512),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `description` LONGTEXT,
    `sample_freq` VARCHAR(512),
    `source_name` VARCHAR(512),
    `sample_freq_name` VARCHAR(512),
    `create_by` VARCHAR(512),
    `create_name` VARCHAR(512),
    `update_by` VARCHAR(512),
    `update_name` VARCHAR(512),
    `urgent` BIGINT DEFAULT 0,
    `start_date` VARCHAR(32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_entrust';

CREATE TABLE IF NOT EXISTS `t_entrust_detail` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `entrust_id` BIGINT,
    `item` VARCHAR(512),
    `standard_id` BIGINT,
    `freq` VARCHAR(512),
    `sample_req` VARCHAR(512),
    `limit_val` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_entrust_detail';

CREATE TABLE IF NOT EXISTS `t_entrust_review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `entrust_id` BIGINT,
    `reviewer_id` BIGINT,
    `opinion` LONGTEXT,
    `result` LONGTEXT,
    `review_at` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_entrust_review';

CREATE TABLE IF NOT EXISTS `t_file_meta` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `biz_type` VARCHAR(512),
    `biz_id` BIGINT,
    `file_name` VARCHAR(512),
    `file_path` VARCHAR(512),
    `hash` VARCHAR(512),
    `size` BIGINT,
    `upload_by` VARCHAR(512),
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_file_meta';

CREATE TABLE IF NOT EXISTS `t_hazardous_flow_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `biz_type` VARCHAR(512),
    `biz_id` BIGINT,
    `event` VARCHAR(512),
    `event_name` VARCHAR(512),
    `from_state` VARCHAR(512),
    `to_state` VARCHAR(512),
    `operator` VARCHAR(512),
    `opinion` LONGTEXT,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_hazardous_flow_log';

CREATE TABLE IF NOT EXISTS `t_hazardous_ledger` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(512),
    `cas_no` VARCHAR(512),
    `category` VARCHAR(512),
    `qty` VARCHAR(512),
    `unit` VARCHAR(512),
    `status` VARCHAR(512),
    `apply_by` VARCHAR(512),
    `approve_by` VARCHAR(512),
    `apply_reason` LONGTEXT,
    `approve_opinion` LONGTEXT,
    `apply_time` DATETIME(6),
    `approve_time` DATETIME(6),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_hazardous_ledger';

CREATE TABLE IF NOT EXISTS `t_instrument` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(512),
    `model` VARCHAR(512),
    `calib_due` VARCHAR(512),
    `status` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `code` VARCHAR(512),
    `manufacturer` VARCHAR(512),
    `purchase_date` DATETIME(6),
    `last_calib_date` DATETIME(6),
    `cert_no` VARCHAR(512),
    `remark` LONGTEXT,
    `create_by` VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_instrument';

CREATE TABLE IF NOT EXISTS `t_instrument_calib` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `instrument_id` BIGINT,
    `calib_date` DATETIME(6),
    `calib_due` VARCHAR(512),
    `cert_no` VARCHAR(512),
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_instrument_calib';

CREATE TABLE IF NOT EXISTS `t_integration_cfg` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `cfg_key` VARCHAR(512) NOT NULL DEFAULT '',
    `cfg_value` LONGTEXT,
    `encrypt_flag` BIGINT DEFAULT 0,
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_integration_cfg';

CREATE TABLE IF NOT EXISTS `t_interlab_compare` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `plan_id` BIGINT,
    `partner_lab` VARCHAR(512),
    `item` VARCHAR(512),
    `standard_id` BIGINT,
    `our_value` LONGTEXT,
    `ref_value` LONGTEXT,
    `deviation` VARCHAR(512),
    `conclusion` LONGTEXT,
    `compare_date` DATETIME(6),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_interlab_compare';

CREATE TABLE IF NOT EXISTS `t_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `to_user` VARCHAR(512),
    `title` VARCHAR(512),
    `content` LONGTEXT,
    `read_flag` BIGINT DEFAULT 0,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_message';

CREATE TABLE IF NOT EXISTS `t_monitor_point` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `cust_id` BIGINT,
    `point_no` VARCHAR(512),
    `point_name` VARCHAR(512) NOT NULL DEFAULT '',
    `lng` DOUBLE,
    `lat` DOUBLE,
    `point_type` VARCHAR(512),
    `condition` VARCHAR(512),
    `history_over_flag` BIGINT DEFAULT 0,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `entrust_id` BIGINT,
    `factors` VARCHAR(512),
    `standard_code` VARCHAR(512),
    `standard_name` VARCHAR(512),
    `freq` VARCHAR(512),
    `point_type_name` VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_monitor_point';

CREATE TABLE IF NOT EXISTS `t_operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `biz_type` VARCHAR(512),
    `biz_id` VARCHAR(512),
    `action` VARCHAR(512),
    `operator_id` VARCHAR(512),
    `op_at` DATETIME(6),
    `detail` LONGTEXT,
    `hash` VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_operation_log';

CREATE TABLE IF NOT EXISTS `t_photo` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `biz_type` VARCHAR(512),
    `biz_id` BIGINT,
    `url` VARCHAR(512),
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_photo';

CREATE TABLE IF NOT EXISTS `t_proficiency_test` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `plan_id` BIGINT,
    `org` VARCHAR(512),
    `item` VARCHAR(512),
    `standard_id` BIGINT,
    `result` LONGTEXT,
    `conclusion` LONGTEXT,
    `cert_file` VARCHAR(512),
    `employee_ids` VARCHAR(512),
    `test_date` DATETIME(6),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_proficiency_test';

CREATE TABLE IF NOT EXISTS `t_qc_activity` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `plan_id` BIGINT,
    `qc_type` VARCHAR(512),
    `item` VARCHAR(512),
    `standard_id` BIGINT,
    `batch_id` BIGINT,
    `result` LONGTEXT,
    `pass_flag` VARCHAR(512),
    `operator_id` VARCHAR(512),
    `act_date` DATETIME(6),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_qc_activity';

CREATE TABLE IF NOT EXISTS `t_qc_plan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `plan_no` VARCHAR(512),
    `title` VARCHAR(512),
    `year` BIGINT,
    `quarter` VARCHAR(512),
    `type` VARCHAR(512),
    `responsible_id` VARCHAR(512),
    `status` VARCHAR(512),
    `approved_by` VARCHAR(512),
    `approved_at` DATETIME(6),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_qc_plan';

CREATE TABLE IF NOT EXISTS `t_repeat_test` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `plan_id` BIGINT,
    `item` VARCHAR(512),
    `standard_id` BIGINT,
    `first_value` LONGTEXT,
    `repeat_value` LONGTEXT,
    `deviation` VARCHAR(512),
    `conclusion` LONGTEXT,
    `operator_id` VARCHAR(512),
    `test_date` DATETIME(6),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_repeat_test';

CREATE TABLE IF NOT EXISTS `t_report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `report_no` VARCHAR(512),
    `title` VARCHAR(512),
    `tpl_id` BIGINT,
    `tpl_type` VARCHAR(512),
    `client` VARCHAR(512),
    `period` VARCHAR(512),
    `task_ids` VARCHAR(512),
    `item_count` BIGINT,
    `exceed_count` BIGINT,
    `status` VARCHAR(512),
    `anti_fake_code` VARCHAR(512),
    `generator` VARCHAR(512),
    `publish_time` DATETIME(6),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_report';

CREATE TABLE IF NOT EXISTS `t_report_audit` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `report_id` BIGINT,
    `auditor` VARCHAR(512),
    `decision` VARCHAR(512),
    `opinion` LONGTEXT,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_report_audit';

CREATE TABLE IF NOT EXISTS `t_report_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `report_id` BIGINT,
    `task_id` BIGINT,
    `item` VARCHAR(512),
    `sample_code` VARCHAR(512),
    `result` LONGTEXT,
    `unit` VARCHAR(512),
    `standard_limit` LONGTEXT,
    `conclusion` LONGTEXT,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_report_item';

CREATE TABLE IF NOT EXISTS `t_report_template` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `tpl_no` VARCHAR(512),
    `name` VARCHAR(512),
    `type` VARCHAR(512),
    `content` LONGTEXT,
    `enabled` VARCHAR(512),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_report_template';

CREATE TABLE IF NOT EXISTS `t_retain` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `sample_id` BIGINT,
    `barcode` VARCHAR(512),
    `name` VARCHAR(512),
    `point_id` BIGINT,
    `retain_by` VARCHAR(512),
    `retain_time` DATETIME(6),
    `retain_days` BIGINT,
    `retain_until` VARCHAR(512),
    `dispose_time` DATETIME(6),
    `dispose_by` VARCHAR(512),
    `status` VARCHAR(512),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `retain_no` VARCHAR(512),
    `category` VARCHAR(512),
    `retain_location` VARCHAR(512),
    `dispose_reason` LONGTEXT,
    `dispose_method` LONGTEXT,
    `dispose_date` DATETIME(6),
    `process_instance_id` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_retain';

CREATE TABLE IF NOT EXISTS `t_rule_def` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `rule_key` VARCHAR(512),
    `rule_name` VARCHAR(512),
    `expr` VARCHAR(512),
    `enabled` BIGINT DEFAULT 1,
    `version` BIGINT DEFAULT 1,
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_rule_def';

CREATE TABLE IF NOT EXISTS `t_sample` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `barcode` VARCHAR(512),
    `sampling_id` BIGINT,
    `order_id` BIGINT,
    `point_id` BIGINT,
    `name` VARCHAR(512),
    `type` VARCHAR(512),
    `source` VARCHAR(512),
    `amount` VARCHAR(512),
    `container` VARCHAR(512),
    `preserve` VARCHAR(512),
    `status` VARCHAR(512),
    `receive_time` DATETIME(6),
    `receive_by` VARCHAR(512),
    `retain_flag` BIGINT,
    `retain_days` BIGINT,
    `retain_until` VARCHAR(512),
    `dispatch_time` DATETIME(6),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `weather` VARCHAR(512),
    `dispatch_id` BIGINT,
    `entrust_id` BIGINT,
    `sample_no` VARCHAR(512),
    `category` VARCHAR(512),
    `item` VARCHAR(512),
    `sample_params` LONGTEXT,
    `preservatives` VARCHAR(512),
    `qc_types` VARCHAR(512),
    `retain_sample` BIGINT DEFAULT 0,
    `sample_photo` VARCHAR(512),
    `retain_by` VARCHAR(512),
    `retain_date` DATETIME(6),
    `retain_location` VARCHAR(512),
    `sampler` VARCHAR(512),
    `sample_time` DATETIME(6),
    `check_items` VARCHAR(512),
    `disposal_type` VARCHAR(512),
    `disposal_method` LONGTEXT,
    `disposal_desc` LONGTEXT,
    `disposal_by` VARCHAR(512),
    `disposal_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_sample';

CREATE TABLE IF NOT EXISTS `t_sample_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `sample_id` BIGINT,
    `action` VARCHAR(512),
    `operator` VARCHAR(512),
    `detail` LONGTEXT,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_sample_log';

CREATE TABLE IF NOT EXISTS `t_sample_param_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `type` VARCHAR(512),
    `item` VARCHAR(512),
    `standard` VARCHAR(512),
    `limit_value` LONGTEXT,
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `unit` VARCHAR(512),
    `inner_limit` VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_sample_param_config';

CREATE TABLE IF NOT EXISTS `t_sample_param_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_id` BIGINT,
    `code` VARCHAR(512),
    `name` VARCHAR(512),
    `param_type` VARCHAR(512),
    `unit` VARCHAR(512),
    `required` BIGINT DEFAULT 1,
    `enum_text` LONGTEXT,
    `tip` LONGTEXT,
    `sort_no` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_sample_param_item';

CREATE TABLE IF NOT EXISTS `t_sample_qc_binding` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `sample_id` BIGINT,
    `sample_no` VARCHAR(512),
    `qc_type` VARCHAR(512),
    `remark` LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_sample_qc_binding';

CREATE TABLE IF NOT EXISTS `t_sampling_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_no` VARCHAR(512),
    `entrust_id` BIGINT,
    `point_id` BIGINT,
    `plan_date` DATETIME(6),
    `sampler_lead` VARCHAR(512),
    `status` VARCHAR(512) DEFAULT '''待派单''',
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_sampling_order';

CREATE TABLE IF NOT EXISTS `t_sampling_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `order_id` BIGINT,
    `dispatch_id` BIGINT,
    `point_id` BIGINT,
    `sampler` VARCHAR(512),
    `sample_time` DATETIME(6),
    `weather` VARCHAR(512),
    `status` VARCHAR(512),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_sampling_record';

CREATE TABLE IF NOT EXISTS `t_seq_def` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `biz_key` VARCHAR(512),
    `prefix` VARCHAR(512),
    `seq_date` DATETIME(6),
    `current_val` BIGINT DEFAULT 0,
    `step` BIGINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_seq_def';

CREATE TABLE IF NOT EXISTS `t_standard_material` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(512),
    `lot_no` VARCHAR(512),
    `spec` VARCHAR(512),
    `expire_date` DATETIME(6),
    `stock` BIGINT,
    `status` VARCHAR(512),
    `cert_no` VARCHAR(512),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_standard_material';

CREATE TABLE IF NOT EXISTS `t_state_def` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `biz_type` VARCHAR(512),
    `state_key` VARCHAR(512),
    `state_name` VARCHAR(512),
    `sort` BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_state_def';

CREATE TABLE IF NOT EXISTS `t_transition_def` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `biz_type` VARCHAR(512),
    `from_state` VARCHAR(512),
    `event` VARCHAR(512),
    `to_state` VARCHAR(512),
    `guard_expr` VARCHAR(512),
    `guard_fail_msg` LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_transition_def';

CREATE TABLE IF NOT EXISTS `t_vehicle` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `plate_no` VARCHAR(512),
    `model` VARCHAR(512),
    `status` BIGINT DEFAULT 1,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `remark` LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_vehicle';

CREATE TABLE IF NOT EXISTS `t_vehicle_maintenance` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `vehicle_id` BIGINT,
    `maint_type` VARCHAR(512),
    `start_date` DATETIME(6),
    `end_date` DATETIME(6),
    `remark` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='t_vehicle_maintenance';

CREATE TABLE IF NOT EXISTS `wf_add_sign_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `original_task_id` BIGINT,
    `add_sign_task_id` BIGINT,
    `process_instance_id` BIGINT,
    `sign_type` VARCHAR(512),
    `initiator` VARCHAR(512),
    `add_sign_user` VARCHAR(512),
    `comment` VARCHAR(512),
    `status` BIGINT DEFAULT 0,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_add_sign_record';

CREATE TABLE IF NOT EXISTS `wf_counter_sign_group` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `process_instance_id` BIGINT,
    `node_id` VARCHAR(512),
    `mode` VARCHAR(512),
    `ratio_threshold` DOUBLE,
    `total_count` BIGINT,
    `approve_count` BIGINT DEFAULT 0,
    `reject_count` BIGINT DEFAULT 0,
    `status` BIGINT DEFAULT 0,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_counter_sign_group';

CREATE TABLE IF NOT EXISTS `wf_counter_sign_vote` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `group_id` BIGINT,
    `voter` VARCHAR(512),
    `vote_result` LONGTEXT,
    `comment` VARCHAR(512),
    `vote_time` DATETIME(6),
    `version` BIGINT DEFAULT 0,
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_counter_sign_vote';

CREATE TABLE IF NOT EXISTS `wf_data_model` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_key` VARCHAR(512),
    `model_name` VARCHAR(512),
    `model_json` LONGTEXT,
    `version` BIGINT,
    `status` BIGINT,
    `source` VARCHAR(32) DEFAULT 'custom' COMMENT '模型来源：builtin-系统内置（只读），custom-用户自定义',
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_data_model';

CREATE TABLE IF NOT EXISTS `wf_delegation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `delegator_id` VARCHAR(512) NOT NULL DEFAULT '',
    `delegate_id` VARCHAR(512) NOT NULL DEFAULT '',
    `start_time` DATETIME(6),
    `end_time` DATETIME(6),
    `reason` LONGTEXT,
    `status` BIGINT DEFAULT 0,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_delegation';

CREATE TABLE IF NOT EXISTS `wf_form_data` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `form_key` VARCHAR(512) NOT NULL DEFAULT '',
    `process_instance_id` BIGINT,
    `task_id` BIGINT,
    `data_json` LONGTEXT,
    `submit_user` VARCHAR(512),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_form_data';

CREATE TABLE IF NOT EXISTS `wf_form_definition` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `form_key` VARCHAR(512),
    `form_name` VARCHAR(512),
    `form_json` LONGTEXT,
    `category` VARCHAR(512),
    `model_key` VARCHAR(512),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_form_definition';

CREATE TABLE IF NOT EXISTS `wf_model_instance` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_key` VARCHAR(512),
    `model_instance_id` VARCHAR(512),
    `process_instance_id` BIGINT,
    `data_json` LONGTEXT,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_model_instance';

CREATE TABLE IF NOT EXISTS `wf_process_definition` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `process_key` VARCHAR(512),
    `process_name` VARCHAR(512),
    `version` BIGINT,
    `process_json` LONGTEXT,
    `category` VARCHAR(512),
    `process_type` VARCHAR(512),
    `description` LONGTEXT,
    `status` BIGINT,
    `deployment_id` VARCHAR(512),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6),
    `create_by` VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_process_definition';

CREATE TABLE IF NOT EXISTS `wf_process_instance` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `instance_no` VARCHAR(512),
    `process_key` VARCHAR(512),
    `process_name` VARCHAR(512),
    `process_version` BIGINT,
    `business_key` VARCHAR(512),
    `status` BIGINT,
    `current_node_id` VARCHAR(512),
    `start_user` VARCHAR(512),
    `start_time` DATETIME(6),
    `end_time` DATETIME(6),
    `duration` BIGINT,
    `version` BIGINT DEFAULT 0,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_process_instance';

CREATE TABLE IF NOT EXISTS `wf_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `process_instance_id` BIGINT,
    `process_key` VARCHAR(512),
    `node_id` VARCHAR(512),
    `node_name` VARCHAR(512),
    `task_type` BIGINT,
    `assignee` VARCHAR(512),
    `candidate_users` VARCHAR(512),
    `claim_time` DATETIME(6),
    `complete_time` DATETIME(6),
    `task_action` BIGINT DEFAULT 0,
    `status` BIGINT,
    `counter_sign_group_id` BIGINT,
    `add_sign_type` VARCHAR(512),
    `sign_type` VARCHAR(512),
    `parent_task_id` BIGINT,
    `reason` LONGTEXT,
    `actual_operator_id` VARCHAR(512),
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_task';

CREATE TABLE IF NOT EXISTS `wf_variable` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `process_instance_id` BIGINT,
    `task_id` BIGINT,
    `variable_key` VARCHAR(512),
    `variable_value` LONGTEXT,
    `variable_type` VARCHAR(512),
    `create_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_variable';

CREATE TABLE IF NOT EXISTS `wf_webhook` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `webhook_key` VARCHAR(512) NOT NULL DEFAULT '',
    `name` VARCHAR(512),
    `url` VARCHAR(512) NOT NULL DEFAULT '',
    `method` LONGTEXT,
    `headers` LONGTEXT,
    `payload_template` LONGTEXT,
    `timeout` BIGINT DEFAULT 5000,
    `retry_count` BIGINT DEFAULT 3,
    `trigger_events` VARCHAR(512),
    `process_key` VARCHAR(512),
    `node_id` VARCHAR(512),
    `status` BIGINT DEFAULT 1,
    `create_time` DATETIME(6),
    `update_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_webhook';

CREATE TABLE IF NOT EXISTS `wf_webhook_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `webhook_id` BIGINT,
    `webhook_key` VARCHAR(512),
    `event_type` VARCHAR(512),
    `process_instance_id` BIGINT,
    `request_url` VARCHAR(512),
    `request_method` LONGTEXT,
    `request_body` LONGTEXT,
    `response_status` BIGINT,
    `response_body` LONGTEXT,
    `status` BIGINT DEFAULT 0,
    `retry_count` BIGINT DEFAULT 0,
    `error_message` LONGTEXT,
    `trigger_time` DATETIME(6),
    `complete_time` DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='wf_webhook_log';

CREATE UNIQUE INDEX idx_form_definition_form_key ON wf_form_definition(`form_key`(255));

CREATE UNIQUE INDEX idx_webhook_key ON wf_webhook(`webhook_key`(255));

CREATE INDEX idx_dm_ddl_test_model_items_main_id ON dm_ddl_test_model_items(`main_id`);

CREATE UNIQUE INDEX idx_api_token_value ON sys_api_token(`token_value`(255));

CREATE UNIQUE INDEX idx_customer_unique ON t_customer(`cust_name`(255), `credit_code`(255));

CREATE INDEX idx_data_dict_type ON t_data_dict(`dict_type`(255));

CREATE UNIQUE INDEX idx_integration_cfg_key ON t_integration_cfg(`cfg_key`(255));

SET FOREIGN_KEY_CHECKS = 1;
