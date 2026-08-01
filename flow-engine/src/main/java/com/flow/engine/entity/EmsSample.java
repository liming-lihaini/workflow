package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 样品（TRD 5.3 样品全生命周期）
 * 状态机：待收样 → 已收样 → 留样中 → 已处置
 */
@Data
@TableName("t_sample")
public class EmsSample {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String barcode;      // 样品条码（YP前缀）
    private Long samplingId;     // 采样记录ID
    private Long orderId;        // 采样订单ID
    private Long pointId;        // 监测点位ID
    private String name;         // 样品名称
    private String type;         // 样品类型（水样/气样/土壤等）
    private String source;       // 样品来源（点位）
    private String amount;       // 数量/规格
    private String container;    // 容器
    private String preserve;     // 保存条件/固定剂
    private String status;       // 待收样/已收样/留样中/已处置
    private String receiveTime;  // 收样时间
    private String receiveBy;    // 收样人
    private Integer retainFlag;  // 是否留样（0/1）
    private Integer retainDays;  // 留样天数
    private String retainUntil;  // 留样到期日
    private String dispatchTime; // 送检/检测下发时间
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
