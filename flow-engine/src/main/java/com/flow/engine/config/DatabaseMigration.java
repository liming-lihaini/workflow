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
