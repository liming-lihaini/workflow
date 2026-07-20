package com.flow.engine.common.enums;

import lombok.Getter;

/**
 * 三员类型枚举（ISSUE-016）
 */
@Getter
public enum AdminType {
    
    SYSTEM_ADMIN(1, "system_admin", "系统管理员"),
    SECURITY_ADMIN(2, "security_admin", "安全管理员"),
    AUDIT_ADMIN(3, "audit_admin", "审计管理员");
    
    private final int code;
    private final String roleKey;
    private final String name;
    
    AdminType(int code, String roleKey, String name) {
        this.code = code;
        this.roleKey = roleKey;
        this.name = name;
    }
    
    /**
     * 根据角色Key获取管理员类型
     */
    public static AdminType fromRoleKey(String roleKey) {
        for (AdminType type : values()) {
            if (type.getRoleKey().equals(roleKey)) {
                return type;
            }
        }
        return null;
    }
}
