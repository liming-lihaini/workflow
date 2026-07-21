package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表单定义实体（ISSUE-008，TRD §2.1.4）
 */
@Data
@TableName("wf_form_definition")
public class FormDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 表单Key（全局唯一） */
    private String formKey;

    /** 表单名称 */
    private String formName;

    /** 表单定义JSON（包含组件列表、布局、校验规则等） */
    private String formJson;

    /** 分类 */
    private String category;

    /** 绑定的数据模型Key */
    private String modelKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
