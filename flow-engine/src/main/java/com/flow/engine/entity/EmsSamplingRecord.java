package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 现场采样记录（TRD 5.3 BR-023-05 采样记录归档）
 * 状态机：采样中 → 采样完成 → 已收样
 */
@Data
@TableName("t_sampling_record")
public class EmsSamplingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;     // 采样任务ID
    private Long dispatchId;  // 派单ID
    private Long pointId;     // 监测点位ID
    private String sampler;   // 采样人
    private String sampleTime; // 采样时间
    private String weather;   // 天气
    private String status;    // 采样中/采样完成/已收样
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
