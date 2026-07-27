package com.flow.engine.config;

import com.flow.engine.service.DelegationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 委托过期定时任务
 * 每分钟检查并自动取消过期委托
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelegationExpireScheduler {

    private final DelegationService delegationService;

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void expireDelegations() {
        try {
            int count = delegationService.expireDelegations();
            if (count > 0) {
                log.info("[DelegationExpireScheduler] 已过期 {} 条委托", count);
            }
        } catch (Exception e) {
            log.error("[DelegationExpireScheduler] 检查过期委托失败", e);
        }
    }
}
