package com.flow.engine.common.enums;

import lombok.Getter;

/**
 * 签批类型：会签 / 加签。
 */
@Getter
public enum SignType {

    COUNTER_SIGN("counterSign", "会签"),
    ADD_SIGN("addSign", "加签");

    private final String code;
    private final String desc;

    SignType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SignType fromCode(String code) {
        for (SignType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
