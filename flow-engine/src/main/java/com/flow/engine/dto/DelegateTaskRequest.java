package com.flow.engine.dto;

import lombok.Data;

/**
 * 委派任务请求
 */
@Data
public class DelegateTaskRequest {

    /** 当前操作人ID */
    private String operatorId;

    /** 受托人ID */
    private String delegateUserId;
}
