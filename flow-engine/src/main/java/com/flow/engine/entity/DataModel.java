package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据模型实体（ISSUE-010，TRD §2.1.5）
 */
@Data
@TableName("wf_data_model")
public class DataModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模型Key（全局唯一） */
    private String modelKey;

    /** 模型名称 */
    private String modelName;

    /** 模型定义JSON（包含主表、子表、计算字段等） */
    private String modelJson;

    /** 版本号 */
    private Integer version;

    /** 状态：0-草稿，1-已发布 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
