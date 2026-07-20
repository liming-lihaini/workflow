package com.flow.engine.dto;

import lombok.Data;

/**
 * 签收任务请求
 */
@Data
public class ClaimTaskRequest {

    /** 签收人ID */
    private String userId;
}
