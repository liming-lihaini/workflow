package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全局委托实体
 */
@Data
@TableName("wf_delegation")
public class Delegation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 委托人ID */
    private String delegatorId;

    /** 代理人ID */
    private String delegateId;

    /** 委托开始时间 */
    private LocalDateTime startTime;

    /** 委托结束时间 */
    private LocalDateTime endTime;

    /** 委托说明 */
    private String reason;

    /** 状态：0-生效中 2-已取消 3-已过期 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
