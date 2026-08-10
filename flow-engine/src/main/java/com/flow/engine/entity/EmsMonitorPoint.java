package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private String pointTypeName;  // 点位类型名称（字典 value）
    /** condition 为 MySQL 保留字，需反引号转义 */
    @TableField("`condition`")
    private String condition;
    /** 监测因子（检测项目，多个用逗号分隔，值取自字典 moni_monitor_factor） */
    private String factors;
    /** 执行标准编号（取自字典 moni_exec_standard） */
    private String standardCode;
    /** 执行标准全称 */
    private String standardName;
    /** 监测频次 / 天数（取自字典 moni_monitor_freq） */
    private String freq;
    private Integer historyOverFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
