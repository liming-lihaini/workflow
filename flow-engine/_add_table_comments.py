#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""为 flow_engine 数据库全部 94 张表补充中文表注释。"""
import pymysql

DB = dict(host="127.0.0.1", port=3306, user="root", password="123456",
          database="flow_engine", charset="utf8mb4")

TABLES = {
    "dm_ddl_test_model_items": "数据模型测试-模型明细项表",
    "dm_ddl_test_model_main": "数据模型测试-模型主表",
    "dm_instrument_t_instrument": "仪器设备台账表(数据模型)",
    "sys_access_log": "系统访问日志表",
    "sys_api_token": "API令牌表",
    "sys_data_permission": "数据权限表",
    "sys_dept": "系统部门表",
    "sys_dict_item": "数据字典项表",
    "sys_dict_type": "数据字典类型表",
    "sys_operation_log": "系统操作日志表",
    "sys_permission": "权限表",
    "sys_role": "角色表",
    "sys_role_permission": "角色权限关联表",
    "sys_user": "用户表",
    "sys_user_post": "用户岗位关联表",
    "sys_user_qualification": "用户资质证书表",
    "sys_user_role": "用户角色关联表",
    "t_alert": "预警消息表",
    "t_asset_scrap": "资产报废记录表",
    "t_consumable": "耗材台账表",
    "t_contract": "合同台账表",
    "t_contract_entrust": "合同委托关联表",
    "t_contract_history": "合同变更历史表",
    "t_contract_node": "合同审批节点配置表",
    "t_contract_txn": "合同审批事务表",
    "t_contract_txn_node": "合同审批事务节点记录表",
    "t_customer": "客户表",
    "t_data_dict": "业务数据字典表",
    "t_department": "业务部门表",
    "t_detection_result": "检测结果表",
    "t_detection_review": "检测复核记录表",
    "t_detection_task": "检测任务表",
    "t_dispatch": "出车任务表",
    "t_dispatch_device": "出车设备关联表",
    "t_dispatch_member": "出车人员关联表",
    "t_employee": "员工表",
    "t_entrust": "检测委托表",
    "t_entrust_detail": "委托明细表(监测点位与项目)",
    "t_entrust_history": "委托变更历史表",
    "t_entrust_review": "委托评审表",
    "t_file_meta": "文件元数据表",
    "t_hazardous_flow_log": "危废流转日志表",
    "t_hazardous_ledger": "危废台账表",
    "t_instrument": "仪器设备台账表",
    "t_instrument_calib": "仪器校准记录表",
    "t_integration_cfg": "集成配置表",
    "t_interlab_compare": "实验室间比对表",
    "t_material_flow": "物料流转记录表",
    "t_message": "消息通知表",
    "t_monitor_point": "监测点位表",
    "t_operation_log": "业务操作日志表",
    "t_overdue_mute": "超期提醒免打扰表",
    "t_photo": "现场照片表",
    "t_proficiency_test": "能力验证表",
    "t_qc_activity": "质控活动表",
    "t_qc_history": "质控历史记录表",
    "t_qc_plan": "质控计划表",
    "t_reminder": "提醒消息表",
    "t_reminder_mute": "提醒免打扰设置表",
    "t_repeat_test": "复检记录表",
    "t_report": "监测报告表",
    "t_report_audit": "报告审核记录表",
    "t_report_item": "报告明细项表",
    "t_report_template": "报告模板表",
    "t_retain": "留样管理表",
    "t_rule_def": "规则引擎规则定义表",
    "t_sample": "样品表",
    "t_sample_log": "样品流转日志表",
    "t_sample_param_config": "样品参数配置表",
    "t_sample_param_item": "样品参数项表",
    "t_sample_qc_binding": "样品质控样绑定表",
    "t_sampling_order": "采样任务表",
    "t_sampling_order_history": "采样任务历史表",
    "t_sampling_record": "采样记录表",
    "t_seq_def": "序列号定义表",
    "t_standard_material": "标准物质表",
    "t_state_def": "状态机状态定义表",
    "t_transition_def": "状态机流转定义表",
    "t_vehicle": "车辆台账表",
    "t_vehicle_maintenance": "车辆维保记录表",
    "wf_add_sign_record": "流程加签记录表",
    "wf_counter_sign_group": "会签分组表",
    "wf_counter_sign_vote": "会签投票表",
    "wf_data_model": "流程数据模型定义表",
    "wf_delegation": "流程委托表",
    "wf_form_data": "表单数据表",
    "wf_form_definition": "表单定义表",
    "wf_model_instance": "数据模型实例表",
    "wf_process_definition": "流程定义表",
    "wf_process_instance": "流程实例表",
    "wf_task": "流程任务表",
    "wf_variable": "流程变量表",
    "wf_webhook": "Webhook配置表",
    "wf_webhook_log": "Webhook调用日志表",
}

conn = pymysql.connect(**DB)
try:
    with conn.cursor() as cur:
        cur.execute("SELECT table_name FROM information_schema.tables "
                    "WHERE table_schema='flow_engine'")
        actual = {r[0] for r in cur.fetchall()}
    missing = actual - set(TABLES)
    extra = set(TABLES) - actual
    if missing:
        print("WARN no comment defined for:", sorted(missing))
    if extra:
        print("WARN dict has unknown tables:", sorted(extra))
    ok = 0
    with conn.cursor() as cur:
        for tbl, cmt in TABLES.items():
            if tbl not in actual:
                continue
            cur.execute("ALTER TABLE `{}` COMMENT='{}'".format(tbl, cmt.replace("'", "''")))
            ok += 1
    conn.commit()
    print("table comments set:", ok)
finally:
    conn.close()
