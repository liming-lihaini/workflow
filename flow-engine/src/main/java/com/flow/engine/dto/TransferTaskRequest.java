package com.flow.engine.dto;

import lombok.Data;

/**
 * 转办任务请求
 */
@Data
public class TransferTaskRequest {

    /** 当前操作人ID */
    private String operatorId;

    /** 目标处理人ID */
    private String targetUserId;
}
