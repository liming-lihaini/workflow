package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 危化品流程流转日志（流程管理数据模型 - 流转记录层）
 * <p>
 * 将危化品台账中原本散落在字段里的审批痕迹（applyBy/approveBy/applyReason/
 * approveOpinion/applyTime/approveTime）解耦为统一的流程事件日志，
 * 由 t_state_def / t_transition_def 状态机驱动，参考 EmsStateMachineService.fire()。
 * 每条记录对应一次状态迁移事件（事件=迁移 code，from=源状态 code，to=目标状态 code）。
 */
@Data
@TableName("t_hazardous_flow_log")
public class EmsHazardousFlowLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务对象类型，固定 hazardous */
    private String bizType = "hazardous";
    /** 关联的危化品台账 id（t_hazardous_ledger.id） */
    private Long bizId;
    /** 触发的迁移事件 code（对应 t_transition_def.transition_code） */
    private String event;
    /** 迁移名称（冗余，便于查询） */
    private String eventName;
    /** 源状态 code（对应 t_state_def.state_code） */
    private String fromState;
    /** 目标状态 code（对应 t_state_def.state_code） */
    private String toState;
    /** 操作人 */
    private String operator;
    /** 审批/操作意见 */
    private String opinion;
    /** 触发时间 */
    private LocalDateTime createTime;
}
