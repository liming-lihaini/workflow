package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 加签请求DTO
 */
@Data
public class AddSignRequest {

    /** 发起人 */
    private String operatorId;

    /** 加签类型：before（前加签）/ after（后加签）/ parallel（并行加签） */
    private String signType;

    /** 被加签人列表 */
    private List<String> targetUsers;

    /** 加签原因 */
    private String reason;
}
