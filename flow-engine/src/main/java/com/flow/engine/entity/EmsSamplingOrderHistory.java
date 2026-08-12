package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采样任务操作历史：记录采样任务的完成等处置轨迹，供调度/详情页「操作记录」展示。
 */
@Data
@TableName("t_sampling_order_history")
public class EmsSamplingOrderHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;       // 采样任务ID
    private String action;      // 操作动作：完成/删除
    private String content;     // 操作内容说明
    private String operatorId;   // 操作人账号
    private String operatorName; // 操作人姓名
    private LocalDateTime createTime;
}
