package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 监测点位（TRD 5.1）
 */
@Data
@TableName("t_monitor_point")
public class EmsMonitorPoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long custId;
    private Long entrustId;   // 归属委托（创建委托时添加点位）
    private String pointNo;
    private String pointName;
    private Double lng;
    private Double lat;
    private String pointType;
    private String condition;
    private Integer historyOverFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
