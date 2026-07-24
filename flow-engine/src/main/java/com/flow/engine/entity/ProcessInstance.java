package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例实体（TRD §2.1.2）
 */
@Data
@TableName("wf_process_instance")
public class ProcessInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程编号：{processKey}-{yyyyMMdd}-{4位流水号} */
    private String instanceNo;

    private String processKey;

    private String processName;

    private Integer processVersion;

    private String businessKey;

    /** 状态：0-运行中，1-已完成，2-已暂停，3-已终止 */
    private Integer status;

    /** 当前节点ID */
    private String currentNodeId;

    private String startUser;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 运行时长(毫秒) */
    private Long duration;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
