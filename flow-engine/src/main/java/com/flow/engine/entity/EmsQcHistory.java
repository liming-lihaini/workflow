package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 质控处置历史：记录质控计划/监控活动的新建、编辑、状态变更、删除等处置轨迹，
 * 供详情页「处置历史」展示。
 */
@Data
@TableName("t_qc_history")
public class EmsQcHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;   // plan-质控计划 / activity-监控活动
    private Long bizId;       // 业务记录ID
    private String action;    // 处置动作：新建/编辑/状态变更/删除
    private String content;   // 处置内容说明（字段变更明细：字段: 旧值 → 新值）
    private String operatorId;   // 处置人账号
    private String operatorName; // 处置人姓名
    private LocalDate createTime;
}
