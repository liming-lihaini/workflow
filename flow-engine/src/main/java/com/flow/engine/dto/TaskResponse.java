package com.flow.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务响应 DTO
 */
@Data
public class TaskResponse {

    private Long id;

    private Long processInstanceId;

    private String processKey;

    /** 流程名称（关联查询） */
    private String processName;

    /** 流程类型（关联查询） */
    private String processType;

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

    /** 节点耗时（秒） */
    private Long duration;

    /** 任务操作：0-正常/1-通过/2-驳回/3-转办/4-委派 */
    private Integer taskAction;

    /** 操作描述 */
    private String taskActionDesc;

    /** 状态：0-待处理，1-处理中，2-已完成 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
