package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体（TRD §2.1.3）
 */
@Data
@TableName("wf_task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long processInstanceId;

    private String processKey;

    private String nodeId;

    private String nodeName;

    /** 任务类型：1-普通任务，2-会签任务，3-加签任务 */
    private Integer taskType;

    /** 处理人 */
    private String assignee;

    /** 候选人（逗号分隔） */
    private String candidateUsers;

    /** 签收时间 */
    private LocalDateTime claimTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 任务操作：0-正常/1-通过/2-驳回/3-转办/4-委派 */
    private Integer taskAction;

    /** 状态：0-待处理，1-处理中，2-已完成 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
