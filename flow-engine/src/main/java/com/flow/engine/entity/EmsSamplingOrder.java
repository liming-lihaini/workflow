package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 环境监测 - 采样任务（TRD 5.1+5.2 状态机核心，BR-023-02 拆单生成）
 */
@Data
@TableName("t_sampling_order")
public class EmsSamplingOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;     // 订单号（CodeGenerator 生成）
    private Long entrustId;
    private Long pointId;       // 点位ID
    private LocalDate planDate; // 计划采样日
    private String samplerLead; // 负责人
    private String status;      // 待派单/已派单/已完成/采样执行中/样品送检/实验室检测中/报告编制/归档完成
    private LocalDateTime actualFinishTime; // 实际完成时间（负责人确认完成时录入）
    private String finishDesc;  // 完成描述（富文本）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
