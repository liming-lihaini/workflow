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
    private Long sampleId;    // 样品ID
    private String barcode;   // 样品条码
    private String name;      // 样品名称
    private Long pointId;     // 点位ID
    private String retainBy;  // 留样人
    private String retainTime;// 留样时间
    private Integer retainDays; // 留样天数
    private String retainUntil; // 留样到期日
    private String disposeTime; // 处置时间
    private String disposeBy; // 处置人
    private String status;    // 留样中/已处置
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
