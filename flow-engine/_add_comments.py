#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""为 flow_engine 数据库所有字段补充 COMMENT 注释。
策略：表.字段专属字典 -> 通用字段字典 -> 字段名兜底。
"""
import pymysql

DB = dict(host="127.0.0.1", port=3306, user="root", password="123456",
          database="flow_engine", charset="utf8mb4")

# ========== 通用字段注释（按字段名，适用所有表） ==========
GENERIC = {
    "id": "主键ID",
    "create_time": "创建时间", "created_time": "创建时间", "created_at": "创建时间",
    "update_time": "更新时间", "updated_time": "更新时间", "updated_at": "更新时间",
    "create_by": "创建人", "created_by": "创建人", "creator": "创建人",
    "update_by": "更新人", "updated_by": "更新人",
    "status": "状态", "remark": "备注", "description": "描述",
    "sort_order": "排序号", "sort": "排序号", "tenant_id": "租户ID",
    "name": "名称", "title": "标题", "type": "类型", "code": "编码",
    "year": "年份", "quarter": "季度", "content": "内容",
    "start_time": "开始时间", "end_time": "结束时间",
    "start_date": "开始日期", "end_date": "结束日期",
    "amount": "数量/金额", "unit": "单位", "opinion": "意见",
    "reason": "原因", "result": "结果", "version": "版本号",
    "parent_id": "父级ID", "operator_id": "操作人", "operator": "操作人",
}

# ========== 表.字段 专属注释 ==========
TABLE = {
    # ---------- 系统管理 ----------
    "sys_user": {"username": "用户名", "password": "密码(加密)", "real_name": "真实姓名",
                 "avatar": "头像", "email": "邮箱", "phone": "手机号", "dept_id": "所属部门ID",
                 "security_level": "密级", "status": "状态(1启用 0禁用)", "last_login_time": "最后登录时间"},
    "sys_role": {"role_name": "角色名称", "role_key": "角色标识", "role_type": "角色类型",
                 "parent_id": "父角色ID", "status": "状态(1启用)"},
    "sys_permission": {"parent_id": "父权限ID", "perm_name": "权限名称", "perm_key": "权限Key",
                       "perm_type": "权限类型(1菜单 2按钮 3数据权限)", "perm_group": "权限分组",
                       "resource_path": "资源路径"},
    "sys_role_permission": {"role_id": "角色ID", "permission_id": "权限ID"},
    "sys_user_role": {"user_id": "用户ID", "role_id": "角色ID"},
    "sys_user_post": {"user_id": "用户ID", "post_id": "岗位ID", "post_name": "岗位名称"},
    "sys_user_qualification": {"user_id": "用户ID", "qual_name": "资质名称", "qual_no": "资质证书编号",
                               "issue_date": "发证日期", "expire_date": "有效期至", "attachment": "附件"},
    "sys_dept": {"dept_name": "部门名称", "leader_id": "部门负责人ID", "leader_name": "部门负责人"},
    "sys_dict_type": {"dict_code": "字典编码", "dict_name": "字典名称"},
    "sys_dict_item": {"dict_code": "所属字典编码", "item_key": "字典项Key", "item_value": "字典项值",
                      "enabled": "是否启用"},
    "sys_data_permission": {"role_id": "角色ID", "data_scope": "数据范围(1全部 2本部门 3本部门及子部门 4仅本人)",
                            "dept_ids": "自定义部门ID集合"},
    "sys_operation_log": {"module": "模块", "operation": "操作", "method": "方法", "params": "请求参数",
                          "user_id": "操作人ID", "username": "操作人用户名", "ip": "IP地址",
                          "duration": "耗时(ms)", "success": "是否成功", "error_msg": "错误信息"},
    "sys_access_log": {"path": "请求路径", "method": "请求方法", "user_id": "访问用户ID",
                       "username": "访问用户名", "ip": "IP地址", "duration": "耗时(ms)",
                       "status_code": "响应状态码"},
    "sys_api_token": {"user_id": "用户ID", "token": "API令牌", "token_name": "令牌名称",
                      "expire_time": "过期时间", "last_used_time": "最后使用时间"},
    # ---------- 流程引擎 ----------
    "wf_process_definition": {"process_key": "流程Key", "process_name": "流程名称", "category": "流程分类",
                              "form_id": "绑定表单ID", "definition_json": "流程定义JSON",
                              "version": "版本", "deployed": "是否部署", "enabled": "是否启用"},
    "wf_process_instance": {"process_key": "流程Key", "definition_id": "流程定义ID",
                            "business_key": "业务Key", "title": "实例标题", "initiator_id": "发起人ID",
                            "initiator_name": "发起人姓名", "current_node_id": "当前节点ID",
                            "variables": "流程变量JSON", "end_time": "结束时间"},
    "wf_task": {"instance_id": "流程实例ID", "node_id": "节点ID", "node_name": "节点名称",
                "assignee_id": "办理人ID", "assignee_name": "办理人姓名", "candidate_ids": "候选办理人ID集合",
                "task_type": "任务类型", "parent_task_id": "父任务ID(加签场景)",
                "comment": "办理意见", "complete_time": "完成时间"},
    "wf_form_definition": {"form_key": "表单Key", "form_name": "表单名称", "form_json": "表单设计JSON",
                           "version": "版本"},
    "wf_form_data": {"instance_id": "流程实例ID", "form_id": "表单ID", "form_json": "表单快照JSON",
                     "form_data": "表单数据JSON"},
    "wf_data_model": {"model_key": "模型Key", "model_name": "模型名称", "table_name": "数据库表名",
                      "model_json": "模型定义JSON", "main_table": "是否主表", "parent_model_id": "父模型ID"},
    "wf_model_instance": {"model_id": "数据模型ID", "instance_id": "流程实例ID", "table_name": "表名",
                          "data_json": "数据JSON"},
    "wf_variable": {"instance_id": "流程实例ID", "var_key": "变量Key", "var_value": "变量值"},
    "wf_webhook": {"definition_id": "流程定义ID", "node_id": "节点ID", "event_type": "事件类型",
                   "url": "回调地址", "method": "请求方法", "headers": "请求头JSON",
                   "body_template": "请求体模板", "enabled": "是否启用", "retry_times": "重试次数"},
    "wf_webhook_log": {"webhook_id": "Webhook ID", "instance_id": "流程实例ID",
                       "request_body": "请求报文", "response_body": "响应报文",
                       "http_status": "HTTP状态码", "success": "是否成功"},
    "wf_delegation": {"delegator_id": "委托人ID", "delegate_id": "受托人ID", "reason": "委托原因"},
    "wf_add_sign_record": {"instance_id": "流程实例ID", "task_id": "原任务ID",
                           "add_sign_type": "加签类型(前加签/后加签)", "added_user_id": "加签办理人ID",
                           "added_user_name": "加签办理人姓名", "operator_id": "操作人ID"},
    "wf_counter_sign_group": {"instance_id": "流程实例ID", "node_id": "会签节点ID",
                              "total_count": "应签人数", "done_count": "已签人数",
                              "pass_rule": "通过规则"},
    "wf_counter_sign_vote": {"group_id": "会签组ID", "task_id": "任务ID", "user_id": "投票人ID",
                             "vote": "投票结果"},
    # ---------- 环境监测：客户/委托 ----------
    "t_customer": {"cust_no": "客户编号", "cust_name": "客户名称", "cust_type": "客户类型",
                   "credit_code": "统一社会信用代码", "legal_person": "法人代表", "contact_person": "联系人",
                   "contact_phone": "联系电话", "address": "地址", "industry": "所属行业"},
    "t_entrust": {"entrust_no": "委托编号", "entrust_name": "委托名称", "cust_id": "客户ID",
                  "cust_name": "客户名称", "source": "委托来源", "urgent": "是否加急",
                  "sign_date": "合同签订日期", "plan_date": "计划监测日期", "sample_freq": "采样频次",
                  "contact_person": "联系人", "contact_phone": "联系电话", "address": "监测地址",
                  "reviewer_id": "技术确认人ID", "review_time": "技术确认时间", "receive_time": "样品接收时间"},
    "t_entrust_detail": {"entrust_id": "委托ID", "point_id": "监测点位ID", "monitor_item": "监测项目",
                         "sample_count": "样品数量", "standard_code": "执行标准"},
    "t_entrust_history": {"entrust_id": "委托ID", "action": "操作动作", "operator_id": "操作人ID",
                          "operator_name": "操作人姓名", "process_instance_id": "关联流程实例ID"},
    "t_entrust_review": {"entrust_id": "委托ID", "review_type": "审核类型", "reviewer_id": "审核人ID",
                         "reviewer_name": "审核人姓名", "review_result": "审核结果"},
    "t_monitor_point": {"entrust_id": "委托ID", "point_no": "点位编号", "point_name": "点位名称",
                        "lng": "经度", "lat": "纬度", "point_type": "点位类型", "factors": "监测因子",
                        "standard_code": "执行标准"},
    # ---------- 采样/样品 ----------
    "t_sampling_order": {"order_no": "采样单编号", "entrust_id": "委托ID", "entrust_name": "委托名称",
                         "plan_start": "计划开始时间", "plan_end": "计划结束时间",
                         "actual_start": "实际开始时间", "actual_end": "实际结束时间"},
    "t_sampling_order_history": {"order_id": "采样单ID", "action": "操作动作",
                                 "operator_name": "操作人姓名"},
    "t_sampling_record": {"order_id": "采样单ID", "point_id": "点位ID", "sample_id": "样品ID",
                          "sample_time": "采样时间", "weather": "天气", "sampler": "采样人"},
    "t_dispatch": {"order_id": "采样单ID", "lead_id": "负责人ID", "lead_name": "负责人姓名",
                   "plan_start": "计划开始时间", "plan_end": "计划结束时间",
                   "vehicle_id": "车辆ID", "dispatch_time": "派单时间"},
    "t_dispatch_member": {"dispatch_id": "派单ID", "emp_id": "成员用户ID", "emp_name": "成员姓名",
                          "role": "角色(LEAD负责人 MEMBER成员)"},
    "t_dispatch_device": {"dispatch_id": "派单ID", "device_id": "设备ID", "device_name": "设备名称"},
    "t_sample": {"barcode": "样品条形码", "sampling_id": "采样记录ID", "order_id": "采样单ID",
                 "point_id": "点位ID", "name": "样品名称", "source": "样品来源",
                 "container": "保存容器", "preserve": "保存方式", "weather": "采样天气",
                 "disposal_type": "处置类型", "disposal_method": "处置方式", "disposal_desc": "处置说明",
                 "disposal_by": "处置人", "disposal_time": "处置时间",
                 "receive_time": "接收日期", "receive_by": "接收人", "retain_flag": "是否留样",
                 "sampler": "采样人", "sample_time": "采样时间"},
    "t_sample_log": {"sample_id": "样品ID", "action": "操作动作", "operator_name": "操作人姓名"},
    "t_sample_param_config": {"monitor_category": "监测类别", "item": "检测项目", "standard": "执行标准",
                              "unit": "单位", "inner_limit": "内控限值"},
    "t_sample_param_item": {"config_id": "配置ID", "item_name": "项目名称"},
    "t_sample_qc_binding": {"sample_id": "样品ID", "qc_type": "质控类型", "qc_sample_id": "质控样品ID"},
    "t_retain": {"sample_id": "样品ID", "location": "留样位置", "retain_date": "留样日期",
                 "expire_date": "到期日期", "destroy_date": "销毁日期", "destroy_by": "销毁人"},
    # ---------- 检测 ----------
    "t_detection_task": {"task_no": "检测任务编号", "sample_id": "样品ID", "sample_barcode": "样品条形码",
                         "monitor_item": "检测项目", "method": "检测方法", "entry_by": "检测人(录入人)",
                         "review_by": "复核人", "review_time": "复核时间", "instrument_id": "使用仪器ID"},
    "t_detection_result": {"task_id": "检测任务ID", "sample_id": "样品ID", "monitor_item": "检测项目",
                           "value": "检测值", "unit": "单位", "limit_value": "标准限值",
                           "conclusion": "结论(达标/超标)", "detect_time": "检测时间"},
    "t_detection_review": {"task_id": "检测任务ID", "reviewer": "复核人", "review_result": "复核结果"},
    "t_repeat_test": {"task_id": "检测任务ID", "reason": "重测原因", "operator_name": "操作人"},
    "t_interlab_compare": {"item": "比对项目", "org_name": "比对机构", "our_value": "我方结果",
                           "their_value": "对方结果", "conclusion": "比对结论"},
    "t_proficiency_test": {"item": "考核项目", "org_name": "组织机构", "z_score": "Z比分",
                           "result": "考核结果"},
    # ---------- 质控 ----------
    "t_qc_plan": {"plan_no": "质控计划编号", "responsible_id": "计划责任人",
                  "approved_by": "审批人", "approved_at": "审批时间"},
    "t_qc_activity": {"plan_id": "质控计划ID", "activity_no": "活动编号", "qc_type": "质控类型",
                      "operator_id": "执行人", "execute_date": "执行日期", "conclusion": "结论"},
    "t_qc_history": {"activity_id": "质控活动ID", "action": "操作动作"},
    # ---------- 报告 ----------
    "t_report": {"report_no": "报告编号", "tpl_id": "模板ID", "tpl_type": "模板类型",
                 "client": "委托单位", "period": "监测周期", "task_ids": "关联检测任务ID集合",
                 "item_count": "检测项数", "exceed_count": "超标项数",
                 "generator": "报告编制人", "publish_time": "发布时间", "cma_cert_no": "CMA证书号",
                 "reviewer": "复核人", "approver": "批准人", "entrust_id": "委托ID"},
    "t_report_item": {"report_id": "报告ID", "task_id": "检测任务ID", "item": "检测项目",
                      "sample_code": "样品编号", "standard_limit": "标准限值", "conclusion": "结论"},
    "t_report_audit": {"report_id": "报告ID", "audit_type": "审核类型(复核/批准)", "auditor": "审核人",
                       "audit_result": "审核结果"},
    "t_report_template": {"tpl_type": "模板类型", "content": "模板内容"},
    # ---------- 合同 ----------
    "t_contract": {"contract_no": "合同编号", "contract_name": "合同名称", "contract_type": "合同类型",
                   "counterparty_id": "对方单位ID", "counterparty_name": "对方单位名称",
                   "amount": "合同金额", "sign_date": "签订日期", "lead_id": "负责人ID",
                   "lead_name": "负责人姓名", "perform_status": "履约状态"},
    "t_contract_entrust": {"contract_id": "合同ID", "entrust_id": "委托ID"},
    "t_contract_history": {"contract_id": "合同ID", "action": "操作动作"},
    "t_contract_node": {"contract_id": "合同ID", "node_name": "节点名称"},
    "t_contract_txn": {"contract_id": "合同ID", "txn_type": "事务类型", "txn_amount": "事务金额"},
    "t_contract_txn_node": {"txn_id": "事务ID", "node_name": "节点名称"},
    # ---------- 资源：车辆/物资/设备 ----------
    "t_vehicle": {"plate_no": "车牌号", "vehicle_type": "车辆类型", "owner": "所有人",
                  "buy_date": "购置日期", "annual_inspect_date": "年检日期"},
    "t_vehicle_maintenance": {"vehicle_id": "车辆ID", "maint_type": "维保类型", "maint_cost": "维保费用",
                              "maint_date": "维保日期"},
    "t_consumable": {"material_no": "物资编号", "material_name": "物资名称", "category": "分类",
                     "stock": "库存数量", "min_stock": "最低库存", "supplier": "供应商",
                     "batch_no": "批次号", "expire_date": "有效期至"},
    "t_material_flow": {"material_id": "物资ID", "flow_type": "出入库类型", "quantity": "数量",
                        "operator_name": "操作人"},
    "t_instrument": {"instrument_no": "设备编号", "instrument_name": "设备名称", "model": "型号",
                     "manufacturer": "生产厂家", "factory_no": "出厂编号", "buy_date": "购置日期",
                     "enable_date": "启用日期", "location": "存放地点", "custodian": "保管人",
                     "calib_cycle": "校准周期(月)", "next_calib_date": "下次校准日期"},
    "t_instrument_calib": {"instrument_id": "设备ID", "calib_type": "校准/检定类型",
                           "calib_date": "校准日期", "valid_until": "有效期至",
                           "calib_org": "校准机构", "certificate_no": "证书编号", "conclusion": "结论"},
    "dm_instrument_t_instrument": {"instrument_no": "设备编号", "instrument_name": "设备名称", "model": "型号",
                                   "manufacturer": "生产厂家", "buy_date": "购置日期", "location": "存放地点"},
    "t_standard_material": {"material_no": "标准物质编号", "material_name": "标准物质名称",
                            "concentration": "浓度/量值", "batch_no": "批次号", "supplier": "供应商",
                            "expire_date": "有效期至", "stock": "库存数量"},
    # ---------- 其他业务 ----------
    "t_employee": {"emp_no": "员工编号", "emp_name": "员工姓名", "dept_id": "部门ID", "post": "岗位",
                   "phone": "联系电话", "cert_no": "持证编号"},
    "t_department": {"dept_name": "部门名称", "dept_code": "部门编码"},
    "t_data_dict": {"dict_type": "字典类型", "dict_key": "字典Key", "dict_value": "字典值"},
    "t_file_meta": {"file_name": "文件名", "file_path": "存储路径", "file_size": "文件大小",
                    "file_type": "文件类型", "biz_type": "业务类型", "biz_id": "业务ID",
                    "uploader": "上传人"},
    "t_photo": {"biz_type": "业务类型", "biz_id": "业务ID", "photo_url": "图片地址",
                "photo_type": "图片类型", "shoot_time": "拍摄时间", "lng": "经度", "lat": "纬度"},
    "t_alert": {"alert_type": "预警类型", "alert_level": "预警级别", "biz_type": "业务类型",
                "biz_id": "业务ID", "alert_msg": "预警内容", "handled": "是否已处理",
                "handle_by": "处理人", "handle_time": "处理时间"},
    "t_message": {"receiver_id": "接收人ID", "msg_type": "消息类型", "msg_content": "消息内容",
                  "biz_type": "业务类型", "biz_id": "业务ID", "read_flag": "是否已读",
                  "read_time": "阅读时间"},
    "t_reminder": {"user_id": "用户ID", "remind_type": "提醒类型", "remind_content": "提醒内容",
                   "biz_type": "业务类型", "biz_id": "业务ID", "handled": "是否已处理"},
    "t_reminder_mute": {"user_id": "用户ID", "remind_type": "提醒类型", "mute_until": "静默截止时间"},
    "t_overdue_mute": {"biz_type": "业务类型", "biz_id": "业务ID", "mute_until": "静默截止时间"},
    "t_asset_scrap": {"asset_type": "资产类型", "asset_id": "资产ID", "asset_name": "资产名称",
                      "scrap_reason": "报废原因", "scrap_date": "报废日期", "applicant": "申请人"},
    "t_hazardous_ledger": {"material_name": "危废名称", "category": "危废类别", "storage_location": "贮存位置",
                           "stock": "库存量"},
    "t_hazardous_flow_log": {"ledger_id": "危废台账ID", "flow_type": "出入库类型", "quantity": "数量",
                             "receiver": "接收单位/人"},
    # ---------- 规则/状态机 ----------
    "t_rule_def": {"rule_key": "规则Key", "rule_name": "规则名称", "rule_expr": "规则表达式",
                   "enabled": "是否启用"},
    "t_state_def": {"biz_type": "业务类型", "state_key": "状态Key", "state_name": "状态名称",
                    "is_final": "是否终态"},
    "t_transition_def": {"state_def_id": "状态定义ID", "event_key": "事件Key", "event_name": "事件名称",
                         "to_state_key": "目标状态Key", "guard_expr": "守卫表达式"},
    "t_seq_def": {"seq_key": "序列Key", "seq_prefix": "编号前缀", "current_val": "当前值",
                  "date_format": "日期格式"},
    "t_operation_log": {"module": "模块", "operation": "操作", "username": "操作人", "ip": "IP地址"},
    "t_integration_cfg": {"cfg_key": "配置Key", "cfg_name": "配置名称", "cfg_value": "配置值",
                          "enabled": "是否启用"},
    "dm_ddl_test_model_main": {"f_text": "文本字段", "f_num": "数字字段", "f_date": "日期字段"},
    "dm_ddl_test_model_items": {"main_id": "主表ID", "f_text": "文本字段", "f_num": "数字字段"},
}


def quote_ident(s):
    return "`" + s.replace("`", "``") + "`"


def quote_comment(s):
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def build_definition(row):
    """根据 information_schema.columns 记录重建列定义"""
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
            parts.append("DEFAULT " + quote_comment(default))
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
        WHERE table_schema = 'flow_engine'
        ORDER BY TABLE_NAME, ORDINAL_POSITION
    """)
    cols = cur.fetchall()
    print("total columns:", len(cols))

    applied, from_table, from_generic, fallback = 0, 0, 0, 0
    cur2 = conn.cursor()
    for r in cols:
        tbl, col = r["TABLE_NAME"], r["COLUMN_NAME"]
        comment = TABLE.get(tbl, {}).get(col)
        if comment:
            from_table += 1
        else:
            comment = GENERIC.get(col)
            if comment:
                from_generic += 1
            else:
                comment = col
                fallback += 1
        ddl = "ALTER TABLE {} MODIFY COLUMN {} COMMENT {}".format(
            quote_ident(tbl), build_definition(r), quote_comment(comment))
        cur2.execute(ddl)
        applied += 1
    conn.commit()
    print("applied={}, table_dict={}, generic={}, fallback={}".format(
        applied, from_table, from_generic, fallback))
    cur2.close()
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
