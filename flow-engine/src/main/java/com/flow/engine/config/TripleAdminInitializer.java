package com.flow.engine.config;

import com.flow.engine.service.TripleAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 三员账号初始化（ISSUE-016）
 * <p>
 * 应用启动时自动初始化三员账号和角色。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(11)
public class TripleAdminInitializer implements CommandLineRunner {

    private final TripleAdminService tripleAdminService;

    @Override
    public void run(String... args) {
        log.info("[TripleAdminInitializer] 开始初始化三员账号...");
        tripleAdminService.initTripleAdmins();
        log.info("[TripleAdminInitializer] 三员账号初始化完成");
    }
}
