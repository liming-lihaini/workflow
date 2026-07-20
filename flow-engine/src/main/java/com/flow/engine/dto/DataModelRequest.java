package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 数据模型创建/更新请求（ISSUE-010）
 */
@Data
public class DataModelRequest {

    /** 模型Key（全局唯一） */
    private String modelKey;

    /** 模型名称 */
    private String modelName;

    /** 主表定义 */
    private TableDefinition mainTable;

    /** 子表定义列表 */
    private List<TableDefinition> subTables;

    /**
     * 表定义
     */
    @Data
    public static class TableDefinition {
        /** 表名 */
        private String tableName;

        /** 表标签 */
        private String label;

        /** 字段列表 */
        private List<FieldDefinition> fields;
    }

    /**
     * 字段定义
     */
    @Data
    public static class FieldDefinition {
        /** 字段Key */
        private String fieldKey;

        /** 字段标签 */
        private String label;

        /** 字段类型：text/number/amount/date/datetime/file/person/department/computed */
        private String type;

        /** 是否必填 */
        private Boolean required;

        /** 默认值 */
        private String defaultValue;

        /** 计算表达式（type=computed时有效），如 "price * quantity" 或 "sum(items.subtotal)" */
        private String expression;

        /** 校验规则 */
        private ValidationRule validation;

        /** 关联引用（如引用其他模型的字段） */
        private String reference;
    }

    /**
     * 校验规则
     */
    @Data
    public static class ValidationRule {
        private String pattern;
        private String patternMessage;
        private Number min;
        private Number max;
        private Integer minLength;
        private Integer maxLength;
    }
}
