package com.flow.engine.config;

import com.flow.engine.common.utils.JsonUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时表结构增量迁移。
 * schema.sql 仅对新建库生效（CREATE TABLE IF NOT EXISTS），已存在的库需在此补齐新增列。
 * 必须先于所有数据初始化器执行（TripleAdminInitializer @Order(11) 等会查询含新列的表）。
 */
@Component
@Order(1)
public class DatabaseMigration implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        // 用户档案扩展字段（性别/出生年月/头像）
        addColumnIfAbsent("sys_user", "gender", "VARCHAR(16)");
        addColumnIfAbsent("sys_user", "birth_date", "VARCHAR(32)");
        addColumnIfAbsent("sys_user", "avatar", "VARCHAR(512)");

        addColumnIfAbsent("t_customer", "city", "TEXT");
        addColumnIfAbsent("t_customer", "address", "TEXT");
        // 委托说明（富文本）
        addColumnIfAbsent("t_entrust", "description", "TEXT");
        // 委托开始日期
        addColumnIfAbsent("t_entrust", "start_date", "VARCHAR(32)");
        // 监测点位归属委托（ISSUE-023 改造：点位作为委托基础信息）
        addColumnIfAbsent("t_monitor_point", "entrust_id", "INTEGER");
        // 点位扩展信息（ISSUE-026：监测因子/执行标准/监测频次/备注）
        addColumnIfAbsent("t_monitor_point", "factors", "TEXT");
        addColumnIfAbsent("t_monitor_point", "standard_code", "VARCHAR(64)");
        addColumnIfAbsent("t_monitor_point", "standard_name", "VARCHAR(255)");
        addColumnIfAbsent("t_monitor_point", "freq", "VARCHAR(64)");

        // 车辆台账补充字段
        addColumnIfAbsent("t_vehicle", "remark", "TEXT");

        // 仪器设备全生命周期字段（TRD 5.5）
        addColumnIfAbsent("t_instrument", "code", "TEXT");
        addColumnIfAbsent("t_instrument", "manufacturer", "TEXT");
        addColumnIfAbsent("t_instrument", "purchase_date", "TEXT");
        addColumnIfAbsent("t_instrument", "last_calib_date", "TEXT");
        addColumnIfAbsent("t_instrument", "cert_no", "TEXT");
        addColumnIfAbsent("t_instrument", "remark", "TEXT");
        // 创建人（入库审批通过时取流程申请人，设备入库 Webhook 写入）
        addColumnIfAbsent("t_instrument", "create_by", "VARCHAR(512)");
        // status 由 INTEGER(1/0) 转为 TEXT（在用/停用），兼容旧数据
        migrateInstrumentStatus();

        // 设备校准历史记录表（TRD 5.5.5）
        createTableIfAbsent("t_instrument_calib",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, instrument_id INTEGER, " +
                "calib_date TEXT, calib_due TEXT, cert_no TEXT, create_time TEXT");

        // 人员资质信息表（后台管理-用户创建/编辑时录入，支持多资质）
        createTableIfAbsent("sys_user_qualification",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT, qual_name TEXT, " +
                "cert_no TEXT, issuer TEXT, expire_date TEXT, remark TEXT, " +
                "create_time TEXT, update_time TEXT");

        // ===== ISSUE-024 采样与样品管理 =====
        createTableIfAbsent("t_sampling_record",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id INTEGER, dispatch_id INTEGER, " +
                "point_id INTEGER, sampler TEXT, sample_time TEXT, weather TEXT, " +
                "status TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_photo",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, biz_type TEXT, biz_id INTEGER, " +
                "url TEXT, create_time TEXT");
        createTableIfAbsent("t_sample",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, barcode TEXT, sampling_id INTEGER, " +
                "order_id INTEGER, point_id INTEGER, name TEXT, type TEXT, source TEXT, " +
                "amount TEXT, container TEXT, preserve TEXT, status TEXT, receive_time TEXT, " +
                "receive_by TEXT, retain_flag INTEGER, retain_days INTEGER, retain_until TEXT, " +
                "dispatch_time TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_sample_qc_binding",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, sample_id INTEGER, sample_no TEXT, " +
                "qc_type TEXT, remark TEXT");
        createTableIfAbsent("t_sample_log",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, sample_id INTEGER, action TEXT, " +
                "operator TEXT, detail TEXT, create_time TEXT");
        createTableIfAbsent("t_retain",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, sample_id INTEGER, barcode TEXT, " +
                "name TEXT, point_id INTEGER, retain_by TEXT, retain_time TEXT, " +
                "retain_days INTEGER, retain_until TEXT, dispose_time TEXT, dispose_by TEXT, " +
                "status TEXT, remark TEXT, create_time TEXT, update_time TEXT");

        // ===== ISSUE-025 检测数据录入与复核 =====
        createTableIfAbsent("t_detection_task",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, task_no TEXT, sample_id INTEGER, " +
                "barcode TEXT, sample_name TEXT, point_id INTEGER, monitor_items TEXT, " +
                "entry_by TEXT, entry_time TEXT, status TEXT, review_by TEXT, review_time TEXT, " +
                "review_opinion TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_detection_result",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, task_id INTEGER, sample_id INTEGER, " +
                "monitor_item TEXT, value TEXT, unit TEXT, method TEXT, limit_value TEXT, " +
                "conclusion TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_detection_review",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, task_id INTEGER, sample_id INTEGER, " +
                "barcode TEXT, reviewer TEXT, decision TEXT, opinion TEXT, create_time TEXT");

        // ===== ISSUE-026 质量控制（物资 + 质控计划 + 能力验证） =====
        createTableIfAbsent("t_standard_material",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, name TEXT, lot_no TEXT, spec TEXT, " +
                "expire_date TEXT, stock INTEGER, status TEXT, cert_no TEXT, remark TEXT, " +
                "create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_consumable",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, name TEXT, spec TEXT, qty INTEGER, " +
                "expire_date TEXT, status TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_hazardous_ledger",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, name TEXT, cas_no TEXT, category TEXT, " +
                "qty TEXT, unit TEXT, status TEXT, apply_by TEXT, approve_by TEXT, " +
                "apply_reason TEXT, approve_opinion TEXT, apply_time TEXT, approve_time TEXT, " +
                "remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_qc_plan",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, plan_no TEXT, title TEXT, year INTEGER, " +
                "quarter TEXT, type TEXT, responsible_id TEXT, status TEXT, approved_by TEXT, " +
                "approved_at TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_qc_activity",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, plan_id INTEGER, qc_type TEXT, item TEXT, " +
                "standard_id INTEGER, batch_id INTEGER, result TEXT, pass_flag TEXT, " +
                "operator_id TEXT, act_date TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_proficiency_test",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, plan_id INTEGER, org TEXT, item TEXT, " +
                "standard_id INTEGER, result TEXT, conclusion TEXT, cert_file TEXT, " +
                "employee_ids TEXT, test_date TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_interlab_compare",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, plan_id INTEGER, partner_lab TEXT, item TEXT, " +
                "standard_id INTEGER, our_value TEXT, ref_value TEXT, deviation TEXT, " +
                "conclusion TEXT, compare_date TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_repeat_test",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, plan_id INTEGER, item TEXT, standard_id INTEGER, " +
                "first_value TEXT, repeat_value TEXT, deviation TEXT, conclusion TEXT, " +
                "operator_id TEXT, test_date TEXT, remark TEXT, create_time TEXT, update_time TEXT");

        // ----- ISSUE-027 报告生成与审核 -----
        createTableIfAbsent("t_report_template",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, tpl_no TEXT, name TEXT, type TEXT, content TEXT, " +
                "enabled TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_report",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, report_no TEXT, title TEXT, tpl_id INTEGER, tpl_type TEXT, " +
                "client TEXT, period TEXT, task_ids TEXT, item_count INTEGER, exceed_count INTEGER, status TEXT, " +
                "anti_fake_code TEXT, generator TEXT, publish_time TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_report_item",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, report_id INTEGER, task_id INTEGER, item TEXT, " +
                "sample_code TEXT, result TEXT, unit TEXT, standard_limit TEXT, conclusion TEXT, create_time TEXT");
        createTableIfAbsent("t_report_audit",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, report_id INTEGER, auditor TEXT, decision TEXT, " +
                "opinion TEXT, create_time TEXT");

        // ----- ISSUE-029 状态机与规则引擎基础底座 -----
        createTableIfAbsent("t_state_def",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, biz_type TEXT, state_key TEXT, state_name TEXT, sort INTEGER");
        createTableIfAbsent("t_transition_def",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, biz_type TEXT, from_state TEXT, event TEXT, " +
                "to_state TEXT, guard_expr TEXT, guard_fail_msg TEXT");
        createTableIfAbsent("t_seq_def",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, biz_key TEXT, prefix TEXT, seq_date TEXT, " +
                "current_val INTEGER DEFAULT 0, step INTEGER DEFAULT 1");

        // ----- 危化品流程管理流转日志（流程管理数据模型 - 流转记录层）-----
        createTableIfAbsent("t_hazardous_flow_log",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, biz_type TEXT, biz_id INTEGER, event TEXT, " +
                "event_name TEXT, from_state TEXT, to_state TEXT, operator TEXT, opinion TEXT, create_time TEXT");

        // 初始化内置状态机/编号（幂等）
        // 注：操作审计复用现有 ISSUE-014 的 sys_operation_log + @OpLog 切面，不再重复建表
        initStateMachineDefs();
        initSeqDefs();
        // 将危化品数据基础表抽象为流程管理-数据模型的一条数据模型记录（可在数据模型模块查看）
        initHazardousDataModel();
        // 将样品留样信息表抽象为流程管理-数据模型的一条数据模型记录（可在数据模型模块查看）
        initRetainDataModel();
        // 将仪器设备台账表抽象为流程管理-数据模型的一条数据模型记录（可在数据模型模块查看）
        initInstrumentDataModel();
        // 注册「修改留样状态」Webhook，绑定到 LYXHSQ 留样销毁流程节点
        initRetainDisposeWebhook();

        // ===== 采样参数配置管理（TRD 5.1） =====
        createTableIfAbsent("t_sample_param_config",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, type TEXT, item TEXT, standard TEXT, "
                + "limit_value TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_sample_param_item",
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, config_id INTEGER, code TEXT, name TEXT, "
                + "param_type TEXT, unit TEXT, required INTEGER DEFAULT 1, enum_text TEXT, "
                + "tip TEXT, sort_no INTEGER");
        // 基于危险品数据模型生成「危险品入库申请」表单定义（绑定 hazardous 模型）
        initHazardousInboundForm();
        // 基于仪器设备数据模型生成「新设备入库申请」表单定义（批量子表版 + 单台版）
        initInstrumentInboundForms();
        // 注册「新设备入库」流程（单台/批量）及审批节点 Webhook（审批通过后写入 t_instrument）
        initInstrumentInboundProcesses();

        // ===== 收样工作台-样品采集（手动收集样品扩展字段） =====
        // 样品关联采样派单/委托单/点位，并存储检测类别、检测项目、采样参数值、
        // 固定剂、现场质控方式、留样标记、现场照片。
        addColumnIfAbsent("t_sample", "dispatch_id", "INTEGER");
        addColumnIfAbsent("t_sample", "entrust_id", "INTEGER");
        addColumnIfAbsent("t_sample", "sample_no", "TEXT");
        addColumnIfAbsent("t_sample", "category", "TEXT");
        addColumnIfAbsent("t_sample", "item", "TEXT");
        addColumnIfAbsent("t_sample", "sample_params", "TEXT");
        addColumnIfAbsent("t_sample", "preservatives", "TEXT");
        addColumnIfAbsent("t_sample", "qc_types", "TEXT");
        addColumnIfAbsent("t_sample", "retain_sample", "INTEGER DEFAULT 0");
        addColumnIfAbsent("t_sample", "sample_photo", "TEXT");
    }

    /**
     * 基于危险品数据模型（modelKey=hazardous）生成「危险品入库申请」表单定义。
     * 表单字段以危化品台账字段（名称/CAS编号/类别/数量/单位/备注）为基底，
     * 补充入库业务字段（存放位置/入库日期/申请人），类别字段引用数据字典 moni_hazardous_category。
     * 幂等：form_key 已存在则跳过。
     */
    private void initHazardousInboundForm() {
        // 表单 schema 遵循设计器/FormRenderer 约定：sections[].children[](rows).cells[].fields
        // 注意：渲染组件遍历的是 section.children（非 rows），字段需落在 cells.fields 内
        String formJson = "{"
                + "\"sections\":[{"
                + "\"id\":\"section_1\",\"title\":\"危险品入库信息\","
                + "\"children\":["
                + "{\"id\":\"row_1\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_1\",\"span\":12,\"fields\":[{\"field\":\"name\",\"label\":\"名称\",\"type\":\"text\",\"required\":true}]},"
                + "{\"id\":\"cell_2\",\"span\":12,\"fields\":[{\"field\":\"casNo\",\"label\":\"CAS编号\",\"type\":\"text\"}]}"
                + "]},"
                + "{\"id\":\"row_2\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_3\",\"span\":12,\"fields\":[{\"field\":\"category\",\"label\":\"类别\",\"type\":\"select\",\"required\":true,\"optionsSource\":\"dict\",\"dictCode\":\"moni_hazardous_category\"}]},"
                + "{\"id\":\"cell_4\",\"span\":12,\"fields\":[{\"field\":\"qty\",\"label\":\"数量\",\"type\":\"number\",\"required\":true}]}"
                + "]},"
                + "{\"id\":\"row_3\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_5\",\"span\":12,\"fields\":[{\"field\":\"unit\",\"label\":\"单位\",\"type\":\"text\"}]},"
                + "{\"id\":\"cell_6\",\"span\":12,\"fields\":[{\"field\":\"location\",\"label\":\"存放位置\",\"type\":\"text\",\"required\":true}]}"
                + "]},"
                + "{\"id\":\"row_4\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_7\",\"span\":12,\"fields\":[{\"field\":\"inboundDate\",\"label\":\"入库日期\",\"type\":\"date\",\"required\":true}]},"
                + "{\"id\":\"cell_8\",\"span\":12,\"fields\":[{\"field\":\"applicant\",\"label\":\"申请人\",\"type\":\"user\",\"required\":true}]}"
                + "]},"
                + "{\"id\":\"row_5\",\"columns\":1,\"cells\":["
                + "{\"id\":\"cell_9\",\"span\":24,\"fields\":[{\"field\":\"remark\",\"label\":\"备注\",\"type\":\"textarea\"}]}"
                + "]}"
                + "]}]}";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int cnt = 0;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM wf_form_definition WHERE form_key='hazardous_inbound'")) {
                if (rs.next()) cnt = rs.getInt(1);
            }
            if (cnt > 0) {
                // 已存在：用最新结构更新（保证 schema 修正生效，可热更）
                st.executeUpdate("UPDATE wf_form_definition SET form_json='" + formJson
                        + "' WHERE form_key='hazardous_inbound'");
            } else {
                st.executeUpdate("INSERT INTO wf_form_definition (form_key, form_name, form_json, category, model_key) "
                        + "VALUES ('hazardous_inbound', '危险品入库申请', '" + formJson + "', 'hazardous', 'hazardous')");
            }
        } catch (Exception ignored) {
            // 幂等保护；表/字段不存在则跳过，不影响主流程
        }
    }

    /**
     * 基于仪器设备数据模型（modelKey=instrument）生成「新设备入库申请」表单定义：
     * 1) instrument_inbound_batch 批量版：子表表格一次录入多台设备，字段继承 instrument 模型主表；
     * 2) instrument_inbound 单台版：平铺字段一次录入一台设备。
     * 幂等：form_key 已存在则用最新结构更新（可热更）。
     */
    private void initInstrumentInboundForms() {
        // 状态下拉选项与设备状态取值一致：在用/临期/停用/维修/报废
        // 注意：MySQL 会对 SQL 字符串字面量做反斜杠转义，此处用 4 个反斜杠保证库中存字面 \n（合法 JSON 转义）
        String statusOptions = "在用:在用\\\\n临期:临期\\\\n停用:停用\\\\n维修:维修\\\\n报废:报废";

        // ===== 1) 批量版：子表表格一次入库多台设备 =====
        String batchFormJson = "{"
                + "\"sections\":[{"
                + "\"id\":\"section_1\",\"title\":\"申请信息\","
                + "\"children\":["
                + "{\"id\":\"row_1\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_1\",\"span\":12,\"fields\":[{\"field\":\"inboundDate\",\"label\":\"入库日期\",\"type\":\"date\",\"required\":true}]},"
                + "{\"id\":\"cell_2\",\"span\":12,\"fields\":[{\"field\":\"applicant\",\"label\":\"申请人\",\"type\":\"user\",\"required\":true}]}"
                + "]},"
                + "{\"id\":\"row_2\",\"columns\":1,\"cells\":["
                + "{\"id\":\"cell_3\",\"span\":24,\"fields\":[{\"field\":\"remark\",\"label\":\"申请说明\",\"type\":\"textarea\"}]}"
                + "]}"
                + "]},{"
                + "\"id\":\"section_2\",\"title\":\"入库设备清单\","
                + "\"children\":["
                + "{\"id\":\"row_3\",\"columns\":1,\"cells\":["
                + "{\"id\":\"cell_4\",\"span\":24,\"fields\":[{"
                + "\"field\":\"devices\",\"label\":\"设备明细\",\"type\":\"subTable\",\"required\":true,\"subTableKey\":\"t_instrument\","
                + "\"columns\":["
                + "{\"fieldKey\":\"code\",\"label\":\"仪器编号\",\"type\":\"text\",\"width\":130},"
                + "{\"fieldKey\":\"name\",\"label\":\"仪器名称\",\"type\":\"text\",\"width\":150},"
                + "{\"fieldKey\":\"model\",\"label\":\"型号\",\"type\":\"text\",\"width\":130},"
                + "{\"fieldKey\":\"manufacturer\",\"label\":\"生产厂商\",\"type\":\"text\",\"width\":150},"
                + "{\"fieldKey\":\"purchaseDate\",\"label\":\"购置日期\",\"type\":\"date\",\"width\":140},"
                + "{\"fieldKey\":\"calibDue\",\"label\":\"校准到期日\",\"type\":\"date\",\"width\":140},"
                + "{\"fieldKey\":\"certNo\",\"label\":\"校准证书编号\",\"type\":\"text\",\"width\":140},"
                + "{\"fieldKey\":\"status\",\"label\":\"状态\",\"type\":\"select\",\"width\":110,\"optionsSource\":\"custom\",\"optionsText\":\"" + statusOptions + "\",\"defaultValue\":\"在用\"},"
                + "{\"fieldKey\":\"remark\",\"label\":\"备注\",\"type\":\"text\",\"width\":150}"
                + "]}]}]}]}]}";

        // ===== 2) 单台版：一次入库一台设备 =====
        String singleFormJson = "{"
                + "\"sections\":[{"
                + "\"id\":\"section_1\",\"title\":\"设备基本信息\","
                + "\"children\":["
                + "{\"id\":\"row_1\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_1\",\"span\":12,\"fields\":[{\"field\":\"code\",\"label\":\"仪器编号\",\"type\":\"text\",\"required\":true}]},"
                + "{\"id\":\"cell_2\",\"span\":12,\"fields\":[{\"field\":\"name\",\"label\":\"仪器名称\",\"type\":\"text\",\"required\":true}]}"
                + "]},"
                + "{\"id\":\"row_2\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_3\",\"span\":12,\"fields\":[{\"field\":\"model\",\"label\":\"型号\",\"type\":\"text\"}]},"
                + "{\"id\":\"cell_4\",\"span\":12,\"fields\":[{\"field\":\"manufacturer\",\"label\":\"生产厂商\",\"type\":\"text\"}]}"
                + "]},"
                + "{\"id\":\"row_3\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_5\",\"span\":12,\"fields\":[{\"field\":\"purchaseDate\",\"label\":\"购置日期\",\"type\":\"date\",\"required\":true}]},"
                + "{\"id\":\"cell_6\",\"span\":12,\"fields\":[{\"field\":\"status\",\"label\":\"状态\",\"type\":\"select\",\"required\":true,\"optionsSource\":\"custom\",\"optionsText\":\"" + statusOptions + "\"}]}"
                + "]}"
                + "]},{"
                + "\"id\":\"section_2\",\"title\":\"校准信息\","
                + "\"children\":["
                + "{\"id\":\"row_4\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_7\",\"span\":12,\"fields\":[{\"field\":\"lastCalibDate\",\"label\":\"上次校准日期\",\"type\":\"date\"}]},"
                + "{\"id\":\"cell_8\",\"span\":12,\"fields\":[{\"field\":\"calibDue\",\"label\":\"校准到期日\",\"type\":\"date\"}]}"
                + "]},"
                + "{\"id\":\"row_5\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_9\",\"span\":12,\"fields\":[{\"field\":\"certNo\",\"label\":\"校准证书编号\",\"type\":\"text\"}]},"
                + "{\"id\":\"cell_10\",\"span\":12,\"fields\":[{\"field\":\"inboundDate\",\"label\":\"入库日期\",\"type\":\"date\",\"required\":true}]}"
                + "]}"
                + "]},{"
                + "\"id\":\"section_3\",\"title\":\"申请信息\","
                + "\"children\":["
                + "{\"id\":\"row_6\",\"columns\":2,\"cells\":["
                + "{\"id\":\"cell_11\",\"span\":12,\"fields\":[{\"field\":\"applicant\",\"label\":\"申请人\",\"type\":\"user\",\"required\":true}]},"
                + "{\"id\":\"cell_12\",\"span\":12,\"fields\":[{\"field\":\"remark\",\"label\":\"备注\",\"type\":\"textarea\"}]}"
                + "]}"
                + "]}]}";

        upsertFormDefinition("instrument_inbound_batch", "新设备入库申请(批量)", batchFormJson, "instrument", "instrument");
        upsertFormDefinition("instrument_inbound", "新设备入库申请(单台)", singleFormJson, "instrument", "instrument");
    }

    /** 幂等写入/热更表单定义（form_key 已存在则更新结构）。 */
    private void upsertFormDefinition(String formKey, String formName, String formJson, String category, String modelKey) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int cnt = 0;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM wf_form_definition WHERE form_key='" + formKey + "'")) {
                if (rs.next()) cnt = rs.getInt(1);
            }
            if (cnt > 0) {
                st.executeUpdate("UPDATE wf_form_definition SET form_json='" + formJson
                        + "', form_name='" + formName + "' WHERE form_key='" + formKey + "'");
            } else {
                st.executeUpdate("INSERT INTO wf_form_definition (form_key, form_name, form_json, category, model_key) "
                        + "VALUES ('" + formKey + "', '" + formName + "', '" + formJson + "', '" + category + "', '" + modelKey + "')");
            }
        } catch (Exception ignored) {
            // 幂等保护；表/字段不存在则跳过，不影响主流程
        }
    }

    /**
     * 将危化品数据基础表（t_hazardous_ledger）注册为流程管理-数据模型中的一条数据模型定义记录。
     * 这样在「流程管理-数据模型」模块即可查看「危险品」数据模型（modelKey=hazardous），
     * 其主表结构即对应 t_hazardous_ledger 的字段。幂等：model_key 已存在则跳过。
     */
    private void initHazardousDataModel() {
        // 类别字段引用数据字典 moni_hazardous_category，供表单设计器绑定模型时自动继承字典来源
        String modelJson = "{\"modelKey\":\"hazardous\",\"modelName\":\"危险品\",\"mainTable\":{"
                + "\"tableName\":\"t_hazardous_ledger\",\"label\":\"危化品台账\",\"fields\":["
                + "{\"fieldKey\":\"name\",\"label\":\"名称\",\"type\":\"text\",\"required\":true},"
                + "{\"fieldKey\":\"casNo\",\"label\":\"CAS编号\",\"type\":\"text\"},"
                + "{\"fieldKey\":\"category\",\"label\":\"类别\",\"type\":\"text\",\"required\":true,\"dictCode\":\"moni_hazardous_category\"},"
                + "{\"fieldKey\":\"qty\",\"label\":\"数量\",\"type\":\"number\"},"
                + "{\"fieldKey\":\"unit\",\"label\":\"单位\",\"type\":\"text\"},"
                + "{\"fieldKey\":\"status\",\"label\":\"状态\",\"type\":\"text\"},"
                + "{\"fieldKey\":\"remark\",\"label\":\"备注\",\"type\":\"text\"}"
                + "]}}";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int cnt = 0;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM wf_data_model WHERE model_key='hazardous'")) {
                if (rs.next()) cnt = rs.getInt(1);
            }
            if (cnt > 0) {
                // 已存在：同步最新模型结构（含类别字典绑定），保证字典继承生效
                st.executeUpdate("UPDATE wf_data_model SET model_json='" + modelJson
                        + "', model_name='危险品', version=1, status=1 WHERE model_key='hazardous'");
            } else {
                st.executeUpdate("INSERT INTO wf_data_model (model_key, model_name, model_json, version, status) "
                        + "VALUES ('hazardous', '危险品', '" + modelJson + "', 1, 1)");
            }
        } catch (Exception ignored) {
            // 幂等保护；若表/字段不存在则跳过，不影响主流程
        }
    }

    /**
     * 将样品留样信息表（t_retain）抽象为流程管理-数据模型的一条数据模型记录，
     * 可在「数据模型」模块查看/绑定留样库业务表单。状态机：留样中 → 销毁审批中 → 已销毁。
     */
    private void initRetainDataModel() {
        String modelJson = "{\"modelKey\":\"retain\",\"modelName\":\"样品留样\",\"mainTable\":{"
                + "\"tableName\":\"t_retain\",\"label\":\"样品留样信息表\",\"fields\":["
                + "{\"fieldKey\":\"id\",\"label\":\"ID\",\"type\":\"number\",\"hidden\":true},"
                + "{\"fieldKey\":\"retainNo\",\"label\":\"留样编号\",\"type\":\"text\",\"required\":true,\"writable\":true,\"columnWidth\":160},"
                + "{\"fieldKey\":\"sampleId\",\"label\":\"样品ID\",\"type\":\"number\",\"columnWidth\":100},"
                + "{\"fieldKey\":\"barcode\",\"label\":\"样品条码\",\"type\":\"text\",\"writable\":true,\"columnWidth\":140},"
                + "{\"fieldKey\":\"name\",\"label\":\"样品名称\",\"type\":\"text\",\"required\":true,\"writable\":true,\"columnWidth\":160},"
                + "{\"fieldKey\":\"category\",\"label\":\"监测类别\",\"type\":\"text\",\"writable\":true,\"columnWidth\":120},"
                + "{\"fieldKey\":\"retainLocation\",\"label\":\"库位\",\"type\":\"text\",\"writable\":true,\"columnWidth\":120},"
                + "{\"fieldKey\":\"pointId\",\"label\":\"点位ID\",\"type\":\"number\",\"columnWidth\":100},"
                + "{\"fieldKey\":\"disposeReason\",\"label\":\"销毁原因\",\"type\":\"textarea\",\"writable\":true,\"columnWidth\":160},"
                + "{\"fieldKey\":\"disposeMethod\",\"label\":\"销毁方式\",\"type\":\"text\",\"writable\":true,\"columnWidth\":120},"
                + "{\"fieldKey\":\"disposeDate\",\"label\":\"预计销毁日期\",\"type\":\"date\",\"writable\":true,\"columnWidth\":140},"
                + "{\"fieldKey\":\"disposeTime\",\"label\":\"实际处置时间\",\"type\":\"datetime\",\"columnWidth\":160},"
                + "{\"fieldKey\":\"processInstanceId\",\"label\":\"流程实例ID\",\"type\":\"number\",\"hidden\":true},"
                + "{\"fieldKey\":\"retainBy\",\"label\":\"留样人\",\"type\":\"text\",\"writable\":true,\"columnWidth\":100},"
                + "{\"fieldKey\":\"retainTime\",\"label\":\"留样时间\",\"type\":\"datetime\",\"columnWidth\":160},"
                + "{\"fieldKey\":\"retainDays\",\"label\":\"留样天数\",\"type\":\"number\",\"writable\":true,\"columnWidth\":100},"
                + "{\"fieldKey\":\"retainUntil\",\"label\":\"留样到期日\",\"type\":\"date\",\"columnWidth\":140},"
                + "{\"fieldKey\":\"disposeBy\",\"label\":\"处置人\",\"type\":\"text\",\"writable\":true,\"columnWidth\":100},"
                + "{\"fieldKey\":\"status\",\"label\":\"留样状态\",\"type\":\"text\",\"required\":true,\"writable\":true,\"columnWidth\":100},"
                + "{\"fieldKey\":\"remark\",\"label\":\"备注\",\"type\":\"textarea\",\"writable\":true,\"columnWidth\":200},"
                + "{\"fieldKey\":\"createTime\",\"label\":\"创建时间\",\"type\":\"datetime\",\"hidden\":true},"
                + "{\"fieldKey\":\"updateTime\",\"label\":\"更新时间\",\"type\":\"datetime\",\"hidden\":true}"
                + "]}}";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int cnt = 0;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM wf_data_model WHERE model_key='retain'")) {
                if (rs.next()) cnt = rs.getInt(1);
            }
            if (cnt > 0) {
                st.executeUpdate("UPDATE wf_data_model SET model_json='" + modelJson
                        + "', model_name='样品留样', version=1, status=1 WHERE model_key='retain'");
            } else {
                st.executeUpdate("INSERT INTO wf_data_model (model_key, model_name, model_json, version, status) "
                        + "VALUES ('retain', '样品留样', '" + modelJson + "', 1, 1)");
            }
        } catch (Exception ignored) {
            // 幂等保护；若表/字段不存在则跳过，不影响主流程
        }
    }

    /**
     * 将仪器设备台账表（t_instrument）抽象为流程管理-数据模型的一条数据模型记录，
     * 可在「数据模型」模块查看/绑定仪器设备业务表单。
     * 字段与 EmsInstrument 实体一致，status 取值：在用/临期/停用/维修/报废。
     */
    private void initInstrumentDataModel() {
        String modelJson = "{\"modelKey\":\"instrument\",\"modelName\":\"仪器设备\",\"mainTable\":{"
                + "\"tableName\":\"t_instrument\",\"label\":\"仪器设备台账\",\"fields\":["
                + "{\"fieldKey\":\"id\",\"label\":\"ID\",\"type\":\"number\",\"hidden\":true},"
                + "{\"fieldKey\":\"code\",\"label\":\"仪器编号\",\"type\":\"text\",\"required\":true,\"writable\":true,\"columnWidth\":140},"
                + "{\"fieldKey\":\"name\",\"label\":\"仪器名称\",\"type\":\"text\",\"required\":true,\"writable\":true,\"columnWidth\":160},"
                + "{\"fieldKey\":\"model\",\"label\":\"型号\",\"type\":\"text\",\"writable\":true,\"columnWidth\":140},"
                + "{\"fieldKey\":\"manufacturer\",\"label\":\"生产厂商\",\"type\":\"text\",\"writable\":true,\"columnWidth\":160},"
                + "{\"fieldKey\":\"purchaseDate\",\"label\":\"购置日期\",\"type\":\"date\",\"writable\":true,\"columnWidth\":120},"
                + "{\"fieldKey\":\"lastCalibDate\",\"label\":\"上次校准日期\",\"type\":\"date\",\"writable\":true,\"columnWidth\":130},"
                + "{\"fieldKey\":\"calibDue\",\"label\":\"校准到期日\",\"type\":\"date\",\"writable\":true,\"columnWidth\":120},"
                + "{\"fieldKey\":\"certNo\",\"label\":\"校准证书编号\",\"type\":\"text\",\"writable\":true,\"columnWidth\":150},"
                + "{\"fieldKey\":\"status\",\"label\":\"状态\",\"type\":\"text\",\"required\":true,\"writable\":true,\"columnWidth\":100},"
                + "{\"fieldKey\":\"remark\",\"label\":\"备注\",\"type\":\"textarea\",\"writable\":true,\"columnWidth\":200},"
                + "{\"fieldKey\":\"createBy\",\"label\":\"创建人\",\"type\":\"text\",\"hidden\":true},"
                + "{\"fieldKey\":\"createTime\",\"label\":\"创建时间\",\"type\":\"datetime\",\"hidden\":true},"
                + "{\"fieldKey\":\"updateTime\",\"label\":\"更新时间\",\"type\":\"datetime\",\"hidden\":true}"
                + "]}}";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int cnt = 0;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM wf_data_model WHERE model_key='instrument'")) {
                if (rs.next()) cnt = rs.getInt(1);
            }
            if (cnt > 0) {
                st.executeUpdate("UPDATE wf_data_model SET model_json='" + modelJson
                        + "', model_name='仪器设备', version=1, status=1 WHERE model_key='instrument'");
            } else {
                st.executeUpdate("INSERT INTO wf_data_model (model_key, model_name, model_json, version, status) "
                        + "VALUES ('instrument', '仪器设备', '" + modelJson + "', 1, 1)");
            }
        } catch (Exception ignored) {
            // 幂等保护；若表/字段不存在则跳过，不影响主流程
        }
    }

    /**
     * 注册「修改留样状态」Webhook 配置，绑定到编号 LYXHSQ 的留样销毁流程节点。
     * 流程节点（NODE_COMPLETED）触发时回调本系统接口 /api/v1/webhooks/retain-status，
     * 由 payloadTemplate 携带流程变量中的留样编号与目標状态，自动更新留样状态。
     * <p>
     * 说明：
     * - node_id 留空表示「流程级 Webhook」，LYXHSQ 的任意节点完成都会触发（如需限定到具体节点，
     *   可在前端 Webhook 配置中将 node_id 设为该流程节点的实际节点ID）。
     * - payloadTemplate 中 ${formData.retainNo} 取自流程变量（LYXHSQ 启动即写入）。
     * - status 目前默认写为「销毁审批中」，可按需在配置中改为其他状态值。
     */
    private void initRetainDisposeWebhook() {
        String webhookKey = "retain_dispose_status";
        String name = "留样销毁-修改留样状态";
        String url = "http://localhost:8080/api/v1/webhooks/retain-status";
        String method = "POST";
        String payloadTemplate = "{\"retainNo\":\"${formData.retainNo}\",\"status\":\"销毁审批中\"}";
        String triggerEvents = "[\"NODE_COMPLETED\"]";
        String processKey = "LYXHSQ";

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int cnt = 0;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM wf_webhook WHERE webhook_key='" + webhookKey + "'")) {
                if (rs.next()) cnt = rs.getInt(1);
            }
            if (cnt > 0) {
                st.executeUpdate("UPDATE wf_webhook SET name='" + name + "', url='" + url + "', method='" + method
                        + "', payload_template='" + payloadTemplate + "', trigger_events='" + triggerEvents
                        + "', process_key='" + processKey + "', node_id=NULL, status=1 WHERE webhook_key='" + webhookKey + "'");
            } else {
                st.executeUpdate("INSERT INTO wf_webhook (webhook_key, name, url, method, payload_template, "
                        + "timeout, retry_count, trigger_events, process_key, node_id, status, create_time, update_time) "
                        + "VALUES ('" + webhookKey + "', '" + name + "', '" + url + "', '" + method + "', '"
                        + payloadTemplate + "', 5000, 3, '" + triggerEvents + "', '" + processKey + "', NULL, 1, "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
            }
        } catch (Exception ignored) {
            // 幂等保护；若 wf_webhook 表不存在则跳过，不影响主流程
        }
    }

    /**
     * 注册「新设备入库申请」流程（单台/批量）及审批节点 Webhook（幂等热更）。
     * <p>
     * 流程结构：开始 -> 设备管理员审批(userTask_approve) -> 结束；
     * 申请人在发起流程时填写入库表单（单台 instrument_inbound / 批量 instrument_inbound_batch）。
     * 审批节点通过（NODE_COMPLETED）时由 Webhook 回调 /api/webhook/instrument/inbound，
     * 将入库设备写入 t_instrument：create_by = 申请人，create_time = 审批通过时间。
     */
    private void initInstrumentInboundProcesses() {
        try {
            upsertInstrumentProcess("SBTKRKSQ", "新设备入库申请(单台)", "instrument_inbound",
                    "单台新设备入库：发起申请 -> 设备管理员审批 -> 审批通过自动登记设备台账");
            upsertInstrumentProcess("SBTKRKSQ_PL", "新设备入库申请(批量)", "instrument_inbound_batch",
                    "批量新设备入库：发起申请(子表录入多台) -> 设备管理员审批 -> 审批通过自动登记设备台账");
            upsertInstrumentWebhook("instrument_inbound_single_hook", "设备入库(单台)-台账登记", "SBTKRKSQ");
            upsertInstrumentWebhook("instrument_inbound_batch_hook", "设备入库(批量)-台账登记", "SBTKRKSQ_PL");
        } catch (Exception ignored) {
            // 幂等保护；若相关表不存在则跳过，不影响主流程
        }
    }

    private void upsertInstrumentProcess(String processKey, String processName, String formKey, String description) throws Exception {
        String processJson = buildInstrumentProcessJson(processKey, processName, formKey);
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int cnt = 0;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM wf_process_definition WHERE process_key='" + processKey + "'")) {
                if (rs.next()) cnt = rs.getInt(1);
            }
            if (cnt > 0) {
                st.executeUpdate("UPDATE wf_process_definition SET process_name='" + processName
                        + "', process_json='" + processJson + "', description='" + description
                        + "', update_time=CURRENT_TIMESTAMP WHERE process_key='" + processKey + "'");
            } else {
                st.executeUpdate("INSERT INTO wf_process_definition (process_key, process_name, version, "
                        + "process_json, category, process_type, description, status, deployment_id, create_time, update_time) "
                        + "VALUES ('" + processKey + "', '" + processName + "', 1, '" + processJson
                        + "', '设备管理', 'approval', '" + description + "', 1, "
                        + "'seed-" + processKey.toLowerCase() + "', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
            }
        }
    }

    /** 构建设备入库流程 JSON：开始 -> 审批(userTask_approve) -> 结束 */
    private String buildInstrumentProcessJson(String processKey, String processName, String formKey) {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("processKey", processKey);
        def.put("processName", processName);
        def.put("formKey", formKey);

        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(buildProcessNode("start_1", "start", "开始", null, null, null, 300, 50, new LinkedHashMap<>()));

        Map<String, Object> approveProps = new LinkedHashMap<>();
        approveProps.put("formKey", formKey);
        // 审批节点上申请人字段只读隐藏（发起时已自动带入）
        Map<String, Object> permFields = new LinkedHashMap<>();
        permFields.put("applicant", "hidden");
        Map<String, Object> formPermissions = new LinkedHashMap<>();
        formPermissions.put("fields", permFields);
        approveProps.put("formPermissions", formPermissions);
        nodes.add(buildProcessNode("userTask_approve", "userTask", "设备管理员审批",
                "sys_admin", "sys_admin", "user", 300, 200, approveProps));

        nodes.add(buildProcessNode("end_1", "end", "结束", null, null, null, 300, 350, new LinkedHashMap<>()));
        def.put("nodes", nodes);

        List<Map<String, Object>> edges = new ArrayList<>();
        edges.add(buildProcessEdge("edge_1", "start_1", "userTask_approve"));
        edges.add(buildProcessEdge("edge_2", "userTask_approve", "end_1"));
        def.put("edges", edges);

        return JsonUtils.toJson(def);
    }

    private Map<String, Object> buildProcessNode(String id, String type, String name, String assignee,
                                                 String candidateUsers, String assigneeType,
                                                 int x, int y, Map<String, Object> properties) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("type", type);
        node.put("name", name);
        node.put("assignee", assignee);
        node.put("candidateUsers", candidateUsers);
        node.put("assigneeType", assigneeType);
        node.put("assigneeDeptId", null);
        node.put("properties", properties);
        node.put("x", x);
        node.put("y", y);
        return node;
    }

    private Map<String, Object> buildProcessEdge(String id, String source, String target) {
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("id", id);
        edge.put("source", source);
        edge.put("target", target);
        edge.put("label", "");
        edge.put("condition", "");
        return edge;
    }

    /**
     * 注册设备入库 Webhook：绑定指定流程的审批节点(userTask_approve)，NODE_COMPLETED 触发。
     * payload_template 留空 -> 发送完整 payload（含 formData：申请人/设备明细），
     * 由 /api/webhook/instrument/inbound 接口写入 t_instrument。
     */
    private void upsertInstrumentWebhook(String webhookKey, String name, String processKey) {
        String url = "http://localhost:8080/api/webhook/instrument/inbound";
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int cnt = 0;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM wf_webhook WHERE webhook_key='" + webhookKey + "'")) {
                if (rs.next()) cnt = rs.getInt(1);
            }
            if (cnt > 0) {
                st.executeUpdate("UPDATE wf_webhook SET name='" + name + "', url='" + url + "', method='POST', "
                        + "payload_template=NULL, trigger_events='[\"NODE_COMPLETED\"]', process_key='" + processKey
                        + "', node_id='userTask_approve', status=1, update_time=CURRENT_TIMESTAMP WHERE webhook_key='" + webhookKey + "'");
            } else {
                st.executeUpdate("INSERT INTO wf_webhook (webhook_key, name, url, method, payload_template, "
                        + "timeout, retry_count, trigger_events, process_key, node_id, status, create_time, update_time) "
                        + "VALUES ('" + webhookKey + "', '" + name + "', '" + url + "', 'POST', NULL, 5000, 3, "
                        + "'[\"NODE_COMPLETED\"]', '" + processKey + "', 'userTask_approve', 1, "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
            }
        } catch (Exception ignored) {
            // 幂等保护
        }
    }

    /** 内置通用校验状态机 + 示例业务状态机定义（幂等插入）。 */
    private void initStateMachineDefs() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            // 通用校验状态机
            ensureTransition(st, "common", "待校验", "verify_pass", "通过", "true", "规则引擎返回 true 才可通过");
            ensureTransition(st, "common", "待校验", "verify_fail", "拒绝", "false", "规则引擎返回 false，记录原因");
            // 委托状态机（示例，供下游复用）
            ensureTransition(st, "entrust", "草稿", "submit", "待技术确认", "true", "提交需基本信息完整");
            ensureTransition(st, "entrust", "待技术确认", "confirm", "已确认", "true", "技术确认通过");
            ensureTransition(st, "entrust", "待技术确认", "reject", "已退回", "true", "退回草稿");
            // 状态定义
            ensureState(st, "common", "待校验", "待校验");
            ensureState(st, "common", "通过", "通过");
            ensureState(st, "common", "拒绝", "拒绝");
            ensureState(st, "entrust", "草稿", "草稿");
            ensureState(st, "entrust", "待技术确认", "待技术确认");
            ensureState(st, "entrust", "已确认", "已确认");
            ensureState(st, "entrust", "已退回", "已退回");

            // 危化品台账审批状态机（bizType=hazardous）
            // 在库 --(apply_use 领用申请)--> 待审批 --(approve_use 审批通过)--> 已领用
            // 在库 --(scrap_apply 报废申请)--> 待审批 --(approve_scrap 审批通过)--> 已报废
            // 待审批 --(reject 审批驳回)--> 在库
            // 先清理旧种子，保证状态机定义始终与代码一致（可热更）
            st.execute("DELETE FROM t_transition_def WHERE biz_type='hazardous'");
            st.execute("DELETE FROM t_state_def WHERE biz_type='hazardous'");
            ensureTransition(st, "hazardous", "在库", "apply_use", "待审批", "#applyReason != null && #applyReason.length() > 0", "领用申请需填写原因");
            ensureTransition(st, "hazardous", "在库", "scrap_apply", "待审批", "#applyReason != null && #applyReason.length() > 0", "报废申请需填写原因");
            ensureTransition(st, "hazardous", "待审批", "approve_use", "已领用", "true", "领用审批通过");
            ensureTransition(st, "hazardous", "待审批", "approve_scrap", "已报废", "true", "报废审批通过");
            ensureTransition(st, "hazardous", "待审批", "reject", "在库", "true", "审批驳回，回退在库");
            ensureState(st, "hazardous", "在库", "在库");
            ensureState(st, "hazardous", "待审批", "待审批");
            ensureState(st, "hazardous", "已领用", "已领用");
            ensureState(st, "hazardous", "已报废", "已报废");
        } catch (Exception ignored) {
        }
    }

    /** 内置编号序列定义（前缀 + 日期段 + 序列）。 */
    private void initSeqDefs() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            ensureSeq(st, "entrust", "WT");
            ensureSeq(st, "order", "WO");
            ensureSeq(st, "sample", "S");
            ensureSeq(st, "batch", "B");
            ensureSeq(st, "report", "RP");
        } catch (Exception ignored) {
        }
    }

    private void ensureTransition(Statement st, String biz, String from, String event,
                                  String to, String guard, String failMsg) throws Exception {
        var rs = st.executeQuery("SELECT 1 FROM t_transition_def WHERE biz_type='" + biz +
                "' AND from_state='" + from + "' AND event='" + event + "'");
        if (!rs.next()) {
            st.execute("INSERT INTO t_transition_def(biz_type,from_state,event,to_state,guard_expr,guard_fail_msg) " +
                    "VALUES('" + biz + "','" + from + "','" + event + "','" + to + "','" + guard + "','" + failMsg + "')");
        }
        rs.close();
    }

    private void ensureState(Statement st, String biz, String key, String name) throws Exception {
        var rs = st.executeQuery("SELECT 1 FROM t_state_def WHERE biz_type='" + biz + "' AND state_key='" + key + "'");
        if (!rs.next()) {
            st.execute("INSERT INTO t_state_def(biz_type,state_key,state_name,sort) VALUES('" + biz + "','" + key + "','" + name + "',0)");
        }
        rs.close();
    }

    private void ensureSeq(Statement st, String biz, String prefix) throws Exception {
        var rs = st.executeQuery("SELECT 1 FROM t_seq_def WHERE biz_key='" + biz + "'");
        String today = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        if (!rs.next()) {
            st.execute("INSERT INTO t_seq_def(biz_key,prefix,seq_date,current_val,step) VALUES('" + biz + "','" + prefix + "','" + today + "',0,1)");
        }
        rs.close();
    }


    private void migrateInstrumentStatus() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            // 仅当 status 存的是数字时转换为文本；SQLite 弱类型，直接 UPDATE 即可
            st.execute("UPDATE t_instrument SET status = '在用' WHERE status = '1' OR status = 1");
            st.execute("UPDATE t_instrument SET status = '停用' WHERE status = '0' OR status = 0");
            st.execute("UPDATE t_instrument SET status = '在用' WHERE status IS NULL OR status = ''");
        } catch (Exception ignored) {
        }
    }

    private void addColumnIfAbsent(String table, String column, String type) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(String.format("ALTER TABLE %s ADD COLUMN %s %s", table, column, type));
        } catch (Exception ignored) {
            // 列已存在或不可添加时忽略
        }
    }

    private void createTableIfAbsent(String table, String columns) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(String.format("CREATE TABLE IF NOT EXISTS %s (%s)", table, columns));
        } catch (Exception ignored) {
            // 表已存在时忽略
        }
    }
}
