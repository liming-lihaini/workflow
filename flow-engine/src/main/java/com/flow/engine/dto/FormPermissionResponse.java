package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 表单权限响应（ISSUE-009，TRD §4.5.1）
 */
@Data
public class FormPermissionResponse {

    /** 表单Key */
    private String formKey;

    /** 节点级权限：edit / readonly / hidden */
    private String nodePermission;

    /** 字段级权限列表 */
    private List<FieldPermission> fieldPermissions;

    /** 按钮级权限列表 */
    private List<ButtonPermission> buttonPermissions;

    /**
     * 字段权限
     */
    @Data
    public static class FieldPermission {
        /** 字段Key */
        private String fieldKey;

        /** 权限：edit / readonly / hidden */
        private String permission;

        /** 条件表达式（可选），如 "amount > 1000" */
        private String condition;

        /** 条件不满足时的默认权限（可选） */
        private String defaultPermission;
    }

    /**
     * 按钮权限
     */
    @Data
    public static class ButtonPermission {
        /** 按钮Key：submit / reject / transfer / delegate 等 */
        private String buttonKey;

        /** 是否可见 */
        private Boolean visible;

        /** 是否可点击 */
        private Boolean enabled;

        /** 条件表达式（可选） */
        private String condition;
    }
}
