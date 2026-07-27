package com.flow.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 委托信息响应
 */
@Data
public class DelegationResponse {

    private Long id;

    /** 委托人ID */
    private String delegatorId;

    /** 委托人姓名 */
    private String delegatorName;

    /** 代理人ID */
    private String delegateId;

    /** 代理人姓名 */
    private String delegateName;

    /** 委托开始时间 */
    private LocalDateTime startTime;

    /** 委托结束时间 */
    private LocalDateTime endTime;

    /** 委托说明 */
    private String reason;

    /** 状态：0-生效中 2-已取消 3-已过期 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
