package com.flow.engine.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 流程完成事件（TRD §5.3）
 */
@Getter
public class ProcessCompletedEvent extends ApplicationEvent {

    private final Long processInstanceId;
    private final String processKey;

    public ProcessCompletedEvent(Object source, Long processInstanceId, String processKey) {
        super(source);
        this.processInstanceId = processInstanceId;
        this.processKey = processKey;
    }
}
