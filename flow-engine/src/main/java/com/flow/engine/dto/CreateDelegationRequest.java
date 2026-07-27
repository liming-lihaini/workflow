package com.flow.engine.dto;

import lombok.Data;

/**
 * 创建全局委托请求
 */
@Data
public class CreateDelegationRequest {

    /** 当前操作人ID（委托人） */
    private String operatorId;

    /** 代理人ID */
    private String delegateUserId;

    /** 委托说明 */
    private String reason;

    /** 委托开始时间 */
    private String startTime;

    /** 委托结束时间 */
    private String endTime;
}
