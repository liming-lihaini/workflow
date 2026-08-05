package com.flow.engine.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 运行时表结构增量迁移。
 * schema.sql 仅对新建库生效（CREATE TABLE IF NOT EXISTS），已存在的库需在此补齐新增列。
 */
@Component
public class DatabaseMigration implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        addColumnIfAbsent("t_customer", "city", "TEXT");
        addColumnIfAbsent("t_customer", "address", "TEXT");
        // 委托说明（富文本）
        addColumnIfAbsent("t_entrust", "description", "TEXT");
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
        // status 由 INTEGER(1/0) 转为 TEXT（在用/停用），兼容旧数据
        migrateInstrumentStatus();

        // 设备校准历史记录表（TRD 5.5.5）
        createTableIfAbsent("t_instrument_calib",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, instrument_id INTEGER, " +
                "calib_date TEXT, calib_due TEXT, cert_no TEXT, create_time TEXT");

        // ===== ISSUE-024 采样与样品管理 =====
        createTableIfAbsent("t_sampling_record",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, dispatch_id INTEGER, " +
                "point_id INTEGER, sampler TEXT, sample_time TEXT, weather TEXT, " +
                "status TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_photo",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, biz_type TEXT, biz_id INTEGER, " +
                "url TEXT, create_time TEXT");
        createTableIfAbsent("t_sample",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, barcode TEXT, sampling_id INTEGER, " +
                "order_id INTEGER, point_id INTEGER, name TEXT, type TEXT, source TEXT, " +
                "amount TEXT, container TEXT, preserve TEXT, status TEXT, receive_time TEXT, " +
                "receive_by TEXT, retain_flag INTEGER, retain_days INTEGER, retain_until TEXT, " +
                "dispatch_time TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_sample_qc_binding",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id INTEGER, sample_no TEXT, " +
                "qc_type TEXT, remark TEXT");
        createTableIfAbsent("t_sample_log",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id INTEGER, action TEXT, " +
                "operator TEXT, detail TEXT, create_time TEXT");
        createTableIfAbsent("t_retain",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, sample_id INTEGER, barcode TEXT, " +
                "name TEXT, point_id INTEGER, retain_by TEXT, retain_time TEXT, " +
                "retain_days INTEGER, retain_until TEXT, dispose_time TEXT, dispose_by TEXT, " +
                "status TEXT, remark TEXT, create_time TEXT, update_time TEXT");

        // ===== ISSUE-025 检测数据录入与复核 =====
        createTableIfAbsent("t_detection_task",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, task_no TEXT, sample_id INTEGER, " +
                "barcode TEXT, sample_name TEXT, point_id INTEGER, monitor_items TEXT, " +
                "entry_by TEXT, entry_time TEXT, status TEXT, review_by TEXT, review_time TEXT, " +
                "review_opinion TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_detection_result",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER, sample_id INTEGER, " +
                "monitor_item TEXT, value TEXT, unit TEXT, method TEXT, limit_value TEXT, " +
                "conclusion TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_detection_review",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER, sample_id INTEGER, " +
                "barcode TEXT, reviewer TEXT, decision TEXT, opinion TEXT, create_time TEXT");

        // ===== ISSUE-026 质量控制（物资 + 质控计划 + 能力验证） =====
        createTableIfAbsent("t_standard_material",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, lot_no TEXT, spec TEXT, " +
                "expire_date TEXT, stock INTEGER, status TEXT, cert_no TEXT, remark TEXT, " +
                "create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_consumable",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, spec TEXT, qty INTEGER, " +
                "expire_date TEXT, status TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_hazardous_ledger",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, cas_no TEXT, category TEXT, " +
                "qty TEXT, unit TEXT, status TEXT, apply_by TEXT, approve_by TEXT, " +
                "apply_reason TEXT, approve_opinion TEXT, apply_time TEXT, approve_time TEXT, " +
                "remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_qc_plan",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, plan_no TEXT, title TEXT, year INTEGER, " +
                "quarter TEXT, type TEXT, responsible_id TEXT, status TEXT, approved_by TEXT, " +
                "approved_at TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_qc_activity",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, plan_id INTEGER, qc_type TEXT, item TEXT, " +
                "standard_id INTEGER, batch_id INTEGER, result TEXT, pass_flag TEXT, " +
                "operator_id TEXT, act_date TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_proficiency_test",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, plan_id INTEGER, org TEXT, item TEXT, " +
                "standard_id INTEGER, result TEXT, conclusion TEXT, cert_file TEXT, " +
                "employee_ids TEXT, test_date TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_interlab_compare",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, plan_id INTEGER, partner_lab TEXT, item TEXT, " +
                "standard_id INTEGER, our_value TEXT, ref_value TEXT, deviation TEXT, " +
                "conclusion TEXT, compare_date TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_repeat_test",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, plan_id INTEGER, item TEXT, standard_id INTEGER, " +
                "first_value TEXT, repeat_value TEXT, deviation TEXT, conclusion TEXT, " +
                "operator_id TEXT, test_date TEXT, remark TEXT, create_time TEXT, update_time TEXT");

        // ----- ISSUE-027 报告生成与审核 -----
        createTableIfAbsent("t_report_template",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, tpl_no TEXT, name TEXT, type TEXT, content TEXT, " +
                "enabled TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_report",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, report_no TEXT, title TEXT, tpl_id INTEGER, tpl_type TEXT, " +
                "client TEXT, period TEXT, task_ids TEXT, item_count INTEGER, exceed_count INTEGER, status TEXT, " +
                "anti_fake_code TEXT, generator TEXT, publish_time TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_report_item",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, report_id INTEGER, task_id INTEGER, item TEXT, " +
                "sample_code TEXT, result TEXT, unit TEXT, standard_limit TEXT, conclusion TEXT, create_time TEXT");
        createTableIfAbsent("t_report_audit",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, report_id INTEGER, auditor TEXT, decision TEXT, " +
                "opinion TEXT, create_time TEXT");

        // ----- ISSUE-029 状态机与规则引擎基础底座 -----
        createTableIfAbsent("t_state_def",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, biz_type TEXT, state_key TEXT, state_name TEXT, sort INTEGER");
        createTableIfAbsent("t_transition_def",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, biz_type TEXT, from_state TEXT, event TEXT, " +
                "to_state TEXT, guard_expr TEXT, guard_fail_msg TEXT");
        createTableIfAbsent("t_rule_def",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, rule_key TEXT, rule_name TEXT, expr TEXT, " +
                "enabled INTEGER DEFAULT 1, version INTEGER DEFAULT 1, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_seq_def",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, biz_key TEXT, prefix TEXT, seq_date TEXT, " +
                "current_val INTEGER DEFAULT 0, step INTEGER DEFAULT 1");

        // ----- 危化品流程管理流转日志（流程管理数据模型 - 流转记录层）-----
        createTableIfAbsent("t_hazardous_flow_log",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, biz_type TEXT, biz_id INTEGER, event TEXT, " +
                "event_name TEXT, from_state TEXT, to_state TEXT, operator TEXT, opinion TEXT, create_time TEXT");

        // 初始化内置状态机/规则/编号（幂等）
        // 注：操作审计复用现有 ISSUE-014 的 sys_operation_log + @OpLog 切面，不再重复建表
        initStateMachineDefs();
        initRuleDefs();
        initSeqDefs();
        // 将危化品数据基础表抽象为流程管理-数据模型的一条数据模型记录（可在数据模型模块查看）
        initHazardousDataModel();

        // ===== 采样参数配置管理（TRD 5.1） =====
        createTableIfAbsent("t_sample_param_config",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, item TEXT, standard TEXT, "
                + "limit_value TEXT, remark TEXT, create_time TEXT, update_time TEXT");
        createTableIfAbsent("t_sample_param_item",
                "id INTEGER PRIMARY KEY AUTOINCREMENT, config_id INTEGER, code TEXT, name TEXT, "
                + "param_type TEXT, unit TEXT, required INTEGER DEFAULT 1, enum_text TEXT, "
                + "tip TEXT, sort_no INTEGER");
        // 基于危险品数据模型生成「危险品入库申请」表单定义（绑定 hazardous 模型）
        initHazardousInboundForm();
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

    /** 内置规则定义（QLExpress/SpEL 等价表达式，可配置热更）。 */
    private void initRuleDefs() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            // 派单资质闸门：context 含 staffQualified、instAvailable
            ensureRule(st, "dispatch_gate", "派单资质闸门",
                    "#staffQualified == true && #instAvailable == true", "派单前校验人员资质与仪器可用");
            // 物资校准闸门：context 含 calibrated
            ensureRule(st, "material_calib_gate", "物资校准闸门",
                    "#calibrated == true", "使用前校验物资/仪器已校准");
            // 超标判定：value > limit
            ensureRule(st, "exceed_judge", "超标判定",
                    "#value > #limit", "检测结果是否超标");
            // 质控判定：偏差在允许范围内
            ensureRule(st, "qc_pass", "质控判定",
                    "#deviation <= #allowDeviation", "质控活动是否通过");
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

    private void ensureRule(Statement st, String key, String name, String expr, String remark) throws Exception {
        var rs = st.executeQuery("SELECT 1 FROM t_rule_def WHERE rule_key='" + key + "'");
        if (!rs.next()) {
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            st.execute("INSERT INTO t_rule_def(rule_key,rule_name,expr,enabled,version,remark,create_time,update_time) " +
                    "VALUES('" + key + "','" + name + "','" + expr + "',1,1,'" + remark + "','" + now + "','" + now + "')");
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
