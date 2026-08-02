package com.flow.engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 采样订单负责人初始化器（ISSUE-026）。
 * 给 sampler_lead 为空的采样订单分配负责人（取自 sys_user.real_name），
 * 使"按负责人分组"具备数据基础。仅填充空值，已分配的不覆盖（幂等）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(14)
public class SamplingOrderDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer emptyCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sampling_order WHERE sampler_lead IS NULL OR sampler_lead = ''", Integer.class);
        if (emptyCnt == null || emptyCnt == 0) {
            log.info("[SamplingOrderDataInitializer] 采样订单负责人均已分配，跳过");
            return;
        }
        List<String> leads = jdbcTemplate.queryForList(
                "SELECT real_name FROM sys_user WHERE real_name IS NOT NULL AND real_name <> '' LIMIT 20", String.class);
        if (leads == null || leads.isEmpty()) {
            log.warn("[SamplingOrderDataInitializer] 未发现可用负责人(sys_user.real_name)，跳过");
            return;
        }
        log.info("[SamplingOrderDataInitializer] 为 {} 条采样订单分配负责人...", emptyCnt);
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM t_sampling_order WHERE sampler_lead IS NULL OR sampler_lead = '' ORDER BY id", Long.class);
        int i = 0;
        for (Long id : ids) {
            String lead = leads.get(i % leads.size());
            jdbcTemplate.update("UPDATE t_sampling_order SET sampler_lead = ? WHERE id = ?", lead, id);
            i++;
        }
        log.info("[SamplingOrderDataInitializer] 负责人分配完成");
    }
}
