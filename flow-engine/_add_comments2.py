#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""第二轮：为仍缺少注释（comment=列名兜底）的字段补充准确中文注释。"""
import pymysql

DB = dict(host="127.0.0.1", port=3306, user="root", password="123456",
          database="flow_engine", charset="utf8mb4")

S = {
    "dm_ddl_test_model_items": {"item_name": "物品名称", "qty": "数量", "subtotal": "小计金额"},
    "dm_ddl_test_model_main": {"apply_date": "申请日期"},
    "dm_instrument_t_instrument": {"purchaseDate": "购置日期", "lastCalibDate": "上次校准日期",
                                   "calibDue": "校准到期日", "certNo": "证书编号",
                                   "createBy": "创建人", "createTime": "创建时间", "updateTime": "更新时间"},
    "sys_access_log": {"user_agent": "客户端标识", "url": "请求地址", "params": "请求参数",
                       "error_msg": "错误信息", "access_time": "访问时间"},
    "sys_api_token": {"token_value": "令牌值"},
    "sys_data_permission": {"dept_id": "部门ID"},
    "sys_dept": {"dept_code": "部门编码", "dept_type": "部门类型", "phone": "联系电话"},
    "sys_dict_item": {"dict_type_id": "字典类型ID", "item_text": "字典项显示文本"},
    "sys_dict_type": {"dict_type": "字典类型"},
    "sys_operation_log": {"before_data": "变更前数据", "after_data": "变更后数据", "operation_time": "操作时间"},
    "sys_user": {"post_id": "岗位ID", "gender": "性别", "birth_date": "出生日期"},
    "sys_user_post": {"dept_id": "部门ID", "is_main": "是否主岗位"},
    "sys_user_qualification": {"cert_no": "证书编号", "issuer": "发证机构"},
    "t_alert": {"level": "预警级别", "msg": "预警消息"},
    "t_asset_scrap": {"spec": "规格型号", "dispose_method": "处置方式", "process_instance_id": "关联流程实例ID"},
    "t_consumable": {"spec": "规格型号", "qty": "数量"},
    "t_contract": {"effect_date": "生效日期", "expire_date": "到期日期", "pay_mode": "付款方式",
                   "create_name": "创建人姓名", "update_name": "更新人姓名"},
    "t_contract_history": {"operator_name": "操作人"},
    "t_contract_node": {"seq": "序号", "plan_amount": "计划金额", "plan_date": "计划日期", "node_desc": "节点说明"},
    "t_contract_txn": {"txn_date": "事务日期", "pay_method": "支付方式", "txn_no": "事务编号", "operator_name": "经办人"},
    "t_contract_txn_node": {"node_id": "合同节点ID", "allocate_amount": "分摊金额"},
    "t_customer": {"contact": "联系人", "tel": "联系电话", "invoice_title": "发票抬头",
                   "tax_no": "税号", "qual_file_id": "资质文件ID", "city": "所在城市"},
    "t_data_dict": {"dict_label": "字典标签"},
    "t_department": {"dept_no": "部门编号", "leader_id": "负责人ID", "leader_name": "负责人"},
    "t_detection_result": {"method": "检测方法", "inner_limit": "内控限值"},
    "t_detection_review": {"sample_id": "样品ID", "barcode": "样品条形码", "decision": "复核结论"},
    "t_detection_task": {"barcode": "样品条形码", "sample_name": "样品名称", "point_id": "监测点位ID",
                         "monitor_items": "检测项目", "entry_time": "录入时间", "review_opinion": "复核意见",
                         "env_temp": "环境温度", "env_humidity": "环境湿度", "conclusion": "检测结论",
                         "attachments": "附件"},
    "t_dispatch": {"note": "派单备注"},
    "t_dispatch_device": {"instrument_id": "仪器设备ID"},
    "t_employee": {"qual_file_id": "资质文件ID", "qual_due": "资质到期日"},
    "t_entrust": {"submit_by": "提交人", "source_name": "委托来源名称", "sample_freq_name": "采样频次名称",
                  "create_name": "创建人姓名", "update_name": "更新人姓名", "process_instance_id": "关联流程实例ID"},
    "t_entrust_detail": {"item": "监测项目", "standard_id": "执行标准ID", "freq": "频次",
                         "sample_req": "采样要求", "limit_val": "限值"},
    "t_entrust_review": {"review_at": "审核时间"},
    "t_file_meta": {"hash": "文件哈希", "size": "文件大小", "upload_by": "上传人"},
    "t_hazardous_flow_log": {"biz_type": "业务类型", "biz_id": "业务ID", "event": "事件",
                             "event_name": "事件名称", "from_state": "源状态", "to_state": "目标状态"},
    "t_hazardous_ledger": {"cas_no": "CAS号", "qty": "数量", "apply_by": "申请人", "approve_by": "审批人",
                           "apply_reason": "申请原因", "approve_opinion": "审批意见",
                           "apply_time": "申请时间", "approve_time": "审批时间"},
    "t_instrument": {"calib_due": "校准到期日", "purchase_date": "购置日期",
                     "last_calib_date": "上次校准日期", "cert_no": "证书编号"},
    "t_instrument_calib": {"calib_due": "校准到期日", "cert_no": "证书编号"},
    "t_integration_cfg": {"encrypt_flag": "是否加密存储"},
    "t_interlab_compare": {"plan_id": "质控计划ID", "partner_lab": "比对实验室", "standard_id": "执行标准ID",
                           "ref_value": "参考值", "deviation": "偏差", "compare_date": "比对日期"},
    "t_material_flow": {"biz_type": "业务类型", "material_type": "物资类型", "spec": "规格型号",
                        "lot_no": "批次号", "qty": "数量", "applicant": "申请人",
                        "process_instance_id": "关联流程实例ID"},
    "t_message": {"to_user": "接收人"},
    "t_monitor_point": {"cust_id": "客户ID", "condition": "工况/排放条件", "history_over_flag": "是否历史超标",
                        "standard_name": "执行标准名称", "freq": "频次", "point_type_name": "点位类型名称"},
    "t_operation_log": {"biz_type": "业务类型", "biz_id": "业务ID", "action": "操作动作",
                        "op_at": "操作时间", "detail": "操作明细", "hash": "数据哈希"},
    "t_overdue_mute": {"user_id": "用户ID", "order_id": "采样单ID", "muted": "是否静默",
                       "snooze_date": "暂缓到期日", "last_popup_time": "最近弹窗时间"},
    "t_photo": {"url": "图片地址"},
    "t_proficiency_test": {"plan_id": "质控计划ID", "org": "组织机构", "standard_id": "考核标准ID",
                           "conclusion": "考核结论", "cert_file": "证书文件",
                           "employee_ids": "参加人员ID集合", "test_date": "考核日期"},
    "t_qc_activity": {"item": "质控项目", "standard_id": "执行标准ID", "batch_id": "批次号",
                      "pass_flag": "是否合格", "act_date": "执行日期", "task_status": "任务状态",
                      "operator_name": "执行人姓名", "task_no": "任务编号", "created_name": "创建人姓名"},
    "t_qc_history": {"biz_type": "业务类型", "biz_id": "业务ID", "operator_name": "操作人"},
    "t_qc_plan": {"created_name": "创建人姓名"},
    "t_reminder": {"source_type": "来源类型", "biz_key": "业务Key", "detail": "提醒明细",
                   "due_date": "到期日期", "owner_id": "责任人ID", "owner_name": "责任人",
                   "resolved": "是否已处理"},
    "t_reminder_mute": {"reminder_id": "提醒ID", "muted": "是否静默", "snooze_date": "暂缓到期日",
                        "last_popup_time": "最近弹窗时间"},
    "t_repeat_test": {"plan_id": "质控计划ID", "item": "复测项目", "standard_id": "执行标准ID",
                      "first_value": "初测值", "repeat_value": "复测值", "deviation": "偏差",
                      "conclusion": "复测结论", "test_date": "测试日期"},
    "t_report": {"anti_fake_code": "防伪码"},
    "t_report_audit": {"decision": "审核决定"},
    "t_report_template": {"tpl_no": "模板编号", "enabled": "是否启用"},
    "t_retain": {"barcode": "样品条形码", "point_id": "监测点位ID", "retain_by": "留样人",
                 "retain_time": "留样时间", "retain_days": "留样天数", "retain_until": "留样到期日",
                 "dispose_time": "处置时间", "dispose_by": "处置人", "retain_no": "留样编号",
                 "category": "留样类别", "retain_location": "留样位置", "dispose_reason": "处置原因",
                 "dispose_method": "处置方式", "dispose_date": "处置日期",
                 "process_instance_id": "关联流程实例ID", "retain_amount": "留样数量"},
    "t_rule_def": {"expr": "规则表达式"},
    "t_sample": {"retain_days": "留样天数", "retain_until": "留样到期日", "dispatch_time": "派单时间",
                 "dispatch_id": "派单ID", "entrust_id": "委托ID", "sample_no": "样品编号",
                 "category": "样品类别", "item": "检测项目", "sample_params": "样品参数",
                 "preservatives": "固定剂", "qc_types": "质控类型", "retain_sample": "是否留样",
                 "sample_photo": "样品照片", "retain_by": "留样人", "retain_date": "留样日期",
                 "retain_location": "留样位置", "check_items": "验收项目", "retain_amount": "留样数量"},
    "t_sample_log": {"detail": "操作明细"},
    "t_sample_param_config": {"limit_value": "限值"},
    "t_sample_param_item": {"param_type": "参数类型", "required": "是否必填", "enum_text": "枚举选项文本",
                            "tip": "提示文本", "sort_no": "排序号"},
    "t_sample_qc_binding": {"sample_no": "样品编号"},
    "t_sampling_order": {"point_id": "监测点位ID", "plan_date": "计划日期", "sampler_lead": "采样负责人",
                         "actual_finish_time": "实际完成时间", "finish_desc": "完成说明",
                         "create_name": "创建人姓名"},
    "t_sampling_record": {"dispatch_id": "派单ID"},
    "t_seq_def": {"biz_key": "业务Key", "prefix": "编号前缀", "seq_date": "序列日期", "step": "递增步长"},
    "t_standard_material": {"lot_no": "批次号", "spec": "规格", "cert_no": "证书编号"},
    "t_transition_def": {"biz_type": "业务类型", "from_state": "源状态", "event": "事件",
                         "to_state": "目标状态", "guard_fail_msg": "守卫失败提示"},
    "t_vehicle": {"model": "车辆型号"},
    "wf_add_sign_record": {"original_task_id": "原任务ID", "add_sign_task_id": "加签任务ID",
                           "process_instance_id": "流程实例ID", "sign_type": "加签类型",
                           "initiator": "发起人", "add_sign_user": "加签办理人", "comment": "加签意见"},
    "wf_counter_sign_group": {"process_instance_id": "流程实例ID", "mode": "会签模式",
                              "ratio_threshold": "通过比例阈值", "approve_count": "同意数",
                              "reject_count": "拒绝数"},
    "wf_counter_sign_vote": {"voter": "投票人", "vote_result": "投票结果", "comment": "意见",
                             "vote_time": "投票时间"},
    "wf_data_model": {"source": "模型来源"},
    "wf_form_data": {"form_key": "表单Key", "process_instance_id": "流程实例ID", "task_id": "任务ID",
                     "data_json": "表单数据JSON", "submit_user": "提交人"},
    "wf_form_definition": {"category": "表单分类", "model_key": "关联数据模型Key"},
    "wf_model_instance": {"model_key": "数据模型Key", "model_instance_id": "模型实例ID",
                          "process_instance_id": "流程实例ID"},
    "wf_process_definition": {"process_json": "流程定义JSON", "process_type": "流程类型",
                              "deployment_id": "部署ID"},
    "wf_process_instance": {"instance_no": "实例编号", "process_name": "流程名称",
                            "process_version": "流程版本", "start_user": "发起人", "duration": "历时"},
    "wf_task": {"process_instance_id": "流程实例ID", "process_key": "流程Key", "assignee": "办理人",
                "candidate_users": "候选办理人", "claim_time": "签收时间", "task_action": "任务动作",
                "counter_sign_group_id": "会签组ID", "add_sign_type": "加签类型", "sign_type": "签署类型",
                "actual_operator_id": "实际操作人ID"},
    "wf_variable": {"process_instance_id": "流程实例ID", "task_id": "任务ID", "variable_key": "变量Key",
                    "variable_value": "变量值", "variable_type": "变量类型"},
    "wf_webhook": {"webhook_key": "Webhook Key", "payload_template": "载荷模板", "timeout": "超时秒数",
                   "retry_count": "重试次数", "trigger_events": "触发事件", "process_key": "流程Key"},
    "wf_webhook_log": {"webhook_key": "Webhook Key", "event_type": "事件类型",
                       "process_instance_id": "流程实例ID", "request_url": "请求地址",
                       "request_method": "请求方法", "response_status": "响应状态码",
                       "retry_count": "重试次数", "error_message": "错误信息",
                       "trigger_time": "触发时间", "complete_time": "完成时间"},
}


def quote_ident(s):
    return "`" + s.replace("`", "``") + "`"


def quote_str(s):
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def build_definition(row):
    parts = [quote_ident(row["COLUMN_NAME"]), row["COLUMN_TYPE"]]
    if row["CHARACTER_SET_NAME"] and row["COLLATION_NAME"]:
        parts.append("CHARACTER SET " + row["CHARACTER_SET_NAME"])
        parts.append("COLLATE " + row["COLLATION_NAME"])
    if row["IS_NULLABLE"] == "NO":
        parts.append("NOT NULL")
    extra = row["EXTRA"] or ""
    default = row["COLUMN_DEFAULT"]
    if default is not None:
        if "DEFAULT_GENERATED" in extra or default.upper().startswith("CURRENT_TIMESTAMP"):
            parts.append("DEFAULT " + default)
        else:
            parts.append("DEFAULT " + quote_str(default))
    if "auto_increment" in extra:
        parts.append("AUTO_INCREMENT")
    if "on update CURRENT_TIMESTAMP" in extra.lower():
        parts.append("ON UPDATE CURRENT_TIMESTAMP")
    return " ".join(parts)


def main():
    conn = pymysql.connect(**DB)
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("""
        SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, CHARACTER_SET_NAME, COLLATION_NAME,
               IS_NULLABLE, COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT
        FROM information_schema.columns
        WHERE table_schema = 'flow_engine' AND column_comment = column_name
    """)
    cols = cur.fetchall()
    print("remaining fallback columns:", len(cols))
    cur2 = conn.cursor()
    fixed, missing = 0, []
    for r in cols:
        tbl, col = r["TABLE_NAME"], r["COLUMN_NAME"]
        comment = S.get(tbl, {}).get(col)
        if not comment:
            missing.append(tbl + "." + col)
            continue
        ddl = "ALTER TABLE {} MODIFY COLUMN {} COMMENT {}".format(
            quote_ident(tbl), build_definition(r), quote_str(comment))
        cur2.execute(ddl)
        fixed += 1
    conn.commit()
    print("fixed:", fixed)
    if missing:
        print("still missing:", missing)
    cur2.close()
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
