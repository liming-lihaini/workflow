package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型实例实体（ISSUE-010，TRD §2.1.6）
 */
@Data
@TableName("wf_model_instance")
public class ModelInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模型Key */
    private String modelKey;

    /** 模型实例ID（UUID） */
    private String modelInstanceId;

    /** 关联流程实例ID */
    private Long processInstanceId;

    /** 实例数据JSON */
    private String dataJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
