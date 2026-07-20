package com.flow.engine.common.enums;

import lombok.Getter;

/**
 * 任务状态（TRD §2.1.3）。
 * 0-待处理，1-处理中，2-已完成
 */
@Getter
public enum TaskStatus {

    PENDING(0, "待处理"),
    IN_PROGRESS(1, "处理中"),
    COMPLETED(2, "已完成");

    private final int value;
    private final String desc;

    TaskStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static TaskStatus fromValue(int value) {
        for (TaskStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}
