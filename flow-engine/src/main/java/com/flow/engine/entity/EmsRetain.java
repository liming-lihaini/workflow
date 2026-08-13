package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 留样库（TRD 5.3 留样与超期预警）
 * 状态机：留样中 → 已处置
 */
@Data
@TableName("t_retain")
public class EmsRetain {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String retainNo;  // 留样编号（LY + yyyyMMdd + 三位序号）
    private Long sampleId;    // 样品ID
    private String barcode;   // 样品条码
    private String name;      // 样品名称
    private String category;  // 监测类别（冗余自 t_sample.category）
    private String retainLocation; // 库位（留样存放位置）
    private Long pointId;     // 点位ID
    /** 销毁相关信息 */
    private String disposeReason;    // 销毁原因
    private String disposeMethod;    // 销毁方式
    private String disposeDate;      // 预计销毁日期
    private String disposeTime;      // 实际销毁/处置时间（流程结束后写入）
    private Long processInstanceId;  // 关联流程实例ID
    private String retainBy;  // 留样人
    private String retainTime;// 留样时间
    private Integer retainDays; // 留样天数
    private String retainAmount; // 留样数量
    private String retainUntil; // 留样到期日
    private String disposeBy; // 处置人
    private String status;    // 留样中/销毁审批中/已销毁
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
