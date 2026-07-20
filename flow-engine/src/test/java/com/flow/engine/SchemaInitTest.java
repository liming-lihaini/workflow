package com.flow.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验收门禁 1：启动后 schema.sql 自动建表，23 张表全部存在。
 */
@SpringBootTest
class SchemaInitTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("25 张表全部自动创建")
    void all19TablesExist() {
        String sql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
        Integer count = jdbc.queryForObject(sql, Integer.class);
        assertThat(count).isEqualTo(25);
    }

    @Test
    @DisplayName("核心业务表 wf_process_definition 可写入")
    void coreTableWritable() {
        jdbc.update("INSERT INTO wf_process_definition (process_name, version, status, process_json) VALUES ('test', 1, 0, '<xml/>')");
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wf_process_definition WHERE process_name='test'", Integer.class);
        assertThat(cnt).isEqualTo(1);
        jdbc.update("DELETE FROM wf_process_definition WHERE process_name='test'");
    }
}
