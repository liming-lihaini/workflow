package com.flow.engine.common.enums;

import lombok.Getter;

/**
 * 任务操作类型。
 * 记录任务是如何被完成的。
 */
@Getter
public enum TaskAction {

    NORMAL(0, "正常"),
    APPROVED(1, "通过"),
    REJECTED(2, "驳回"),
    TRANSFERRED(3, "转办"),
    DELEGATED(4, "委派"),
    CANCELLED(5, "取消"),
    ADD_SIGNED(6, "加签");

    private final int value;
    private final String desc;

    TaskAction(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static TaskAction fromValue(int value) {
        for (TaskAction action : values()) {
            if (action.value == value) {
                return action;
            }
        }
        return NORMAL;
    }
}
