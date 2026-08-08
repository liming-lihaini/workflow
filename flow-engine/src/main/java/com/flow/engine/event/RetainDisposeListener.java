package com.flow.engine.event;

import com.flow.engine.entity.EmsRetain;
import com.flow.engine.service.EmsRetainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 留样销毁流程完成监听器
 * 当 LYXHSQ 流程正常完成时，更新留样状态为"已销毁"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetainDisposeListener {

    private final EmsRetainService retainService;

    @EventListener
    public void onProcessCompleted(ProcessCompletedEvent event) {
        if (!"LYXHSQ".equals(event.getProcessKey())) return;
        Long instanceId = event.getProcessInstanceId();
        log.info("[RetainDispose] 留样销毁流程完成，instanceId={}", instanceId);

        // 通过流程实例ID找到留样记录
        EmsRetain retain = retainService.lambdaQuery()
                .eq(EmsRetain::getProcessInstanceId, instanceId)
                .one();
        if (retain == null) {
            log.warn("[RetainDispose] 未找到关联留样记录，instanceId={}", instanceId);
            return;
        }

        // 更新状态为已销毁
        retain.setStatus("已销毁");
        retain.setDisposeTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        retain.setUpdateTime(LocalDateTime.now());
        retainService.updateById(retain);
        log.info("[RetainDispose] 留样 {} 已标记为已销毁", retain.getRetainNo());
    }
}
