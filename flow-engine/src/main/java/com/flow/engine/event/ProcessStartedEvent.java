package com.flow.engine.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 流程启动事件（TRD §5.3）
 */
@Getter
public class ProcessStartedEvent extends ApplicationEvent {

    private final Long processInstanceId;
    private final String processKey;
    private final String startUser;

    public ProcessStartedEvent(Object source, Long processInstanceId, String processKey, String startUser) {
        super(source);
        this.processInstanceId = processInstanceId;
        this.processKey = processKey;
        this.startUser = startUser;
    }
}
