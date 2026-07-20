package com.flow.engine.common.enums;

import lombok.Getter;

/**
 * 会签模式（PRD §会签 / TRD §4.2）。
 */
@Getter
public enum CounterSignMode {

    ALL_PASS("allPass", "全票通过"),
    RATIO("ratio", "按比例通过"),
    ONE_PASS("onePass", "一票通过"),
    ONE_REJECT("oneReject", "一票否决");

    private final String code;
    private final String desc;

    CounterSignMode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CounterSignMode fromCode(String code) {
        for (CounterSignMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        return null;
    }
}
