package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义实体（TRD §2.1.1）
 */
@Data
@TableName("wf_process_definition")
public class ProcessDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String processKey;

    private String processName;

    private Integer version;

    private String processJson;

    private String category;

    /** 状态：0-草稿，1-已部署 */
    private Integer status;

    private String deploymentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String createBy;
}
