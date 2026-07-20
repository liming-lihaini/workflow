package com.flow.engine.common.enums;

import lombok.Getter;

import java.util.Set;
import java.util.Map;

/**
 * 流程实例状态（TRD §5.1 状态机）。
 * <p>DB 存储为 INTEGER：0-运行中，1-已完成，2-已暂停，3-已终止
 */
@Getter
public enum ProcessStatus {

    RUNNING(0, "运行中"),
    COMPLETED(1, "已完成"),
    SUSPENDED(2, "已暂停"),
    TERMINATED(3, "已终止");

    private final int value;
    private final String desc;

    ProcessStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ProcessStatus fromValue(int value) {
        for (ProcessStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    /** 合法状态跃迁表（TRD §5.1） */
    private static final Map<ProcessStatus, Set<ProcessStatus>> TRANSITIONS = Map.of(
            RUNNING, Set.of(COMPLETED, SUSPENDED, TERMINATED),
            SUSPENDED, Set.of(RUNNING, TERMINATED)
    );

    /**
     * 判断是否可以从当前状态跃迁到目标状态。
     * TERMINATED 和 COMPLETED 为终态，不可再跃迁。
     */
    public boolean canTransitionTo(ProcessStatus target) {
        Set<ProcessStatus> allowed = TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }
}
