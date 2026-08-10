package com.flow.engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 委托单测试数据初始化器（ISSUE-026）。
 * 当 t_ems_entrust 数量 < 100 时，关联已有客户初始化 100 条委托数据。
 * 仅插入安全状态（草稿/已退回）以免触发状态机（待技术确认等需拆单）。
 * 在 CustomerDataInitializer(@Order 11) 之后执行，保证有客户可关联。
 */
@Slf4j
@RequiredArgsConstructor
@Order(12)
public class EntrustDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private static final int TARGET = 100;

    private static final String[] NAME_CORE = {
        "废水例行监测", "废气监督监测", "土壤隐患排查", "噪声厂界监测", "地表水环境质量",
        "地下水监测", "环境空气质量", "食堂油烟检测", "生活污水监测", "无组织废气监测",
        "饮用水源监测", "电磁辐射监测", "固废浸出毒性", "环境现状监测", "验收监测"
    };
    private static final String[] NAME_SUFFIX = {
        "委托", "检测", "监测项目", "一阶段", "二阶段", "年度", "季度", "专项"
    };
    private static final String[] SOURCES = { "委托", "送检", "抽检" };
    private static final String[] STATUSES = { "草稿", "已退回" };
    private static final String[] REMARKS = {
        "按年度监测计划执行", "配合环保督察整改", "客户自主委托", "例行巡检", "新建项目验收"
    };

    @Override
    public void run(String... args) {
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_entrust", Integer.class);
        if (cnt != null && cnt >= TARGET) {
            log.info("[EntrustDataInitializer] t_entrust 已存在 {} 条，跳过初始化", cnt);
            return;
        }
        if (cnt != null && cnt > 0) {
            log.info("[EntrustDataInitializer] t_entrust 现有 {} 条，不足 {} 条，先清空再初始化", cnt, TARGET);
            jdbcTemplate.update("DELETE FROM t_entrust");
        }

        List<Long> custIds = jdbcTemplate.queryForList("SELECT id FROM t_customer", Long.class);
        if (custIds == null || custIds.isEmpty()) {
            log.warn("[EntrustDataInitializer] 未发现客户数据，跳过委托初始化");
            return;
        }

        log.info("[EntrustDataInitializer] 开始初始化 {} 条委托数据...", TARGET);
        Random rand = new Random(20260802L);
        int inserted = 0;
        for (int i = 1; i <= TARGET; i++) {
            String entrustNo = String.format("WT%08d", 20260000 + i);
            String name = NAME_CORE[(i - 1) % NAME_CORE.length] + NAME_SUFFIX[(i * 5) % NAME_SUFFIX.length];
            Long custId = custIds.get(rand.nextInt(custIds.size()));
            String source = SOURCES[i % SOURCES.length];
            String status = STATUSES[i % STATUSES.length];
            String remark = REMARKS[i % REMARKS.length];
            jdbcTemplate.update(
                    "INSERT INTO t_entrust (entrust_name, entrust_no, cust_id, source, status, description, create_time, update_time) "
                            + "VALUES (?,?,?,?,?,?,datetime('now'),datetime('now'))",
                    name, entrustNo, custId, source, status, remark);
            inserted++;
        }
        log.info("[EntrustDataInitializer] 委托数据初始化完成: 新增 {} 条", inserted);
    }
}
