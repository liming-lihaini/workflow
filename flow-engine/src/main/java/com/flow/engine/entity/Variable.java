package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程变量实体（TRD §2.1.7）
 */
@Data
@TableName("wf_variable")
public class Variable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long processInstanceId;

    private Long taskId;

    private String variableKey;

    private String variableValue;

    private String variableType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
