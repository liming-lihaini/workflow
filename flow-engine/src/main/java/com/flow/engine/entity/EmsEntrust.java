package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 委托单主数据框架（TRD 5.1，完整状态机见 ISSUE-023）
 */
@Data
@TableName("t_entrust")
public class EmsEntrust {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String entrustNo;
    private Long custId;
    private String entrustName;
    private String source;
    private String status;
    private String description;  // 委托说明（富文本）
    private String sampleFreq;    // 委托级采集频率（字典 moni_sample_freq 编码）
    private String sampleFreqName; // 采集频率名称（字典 value，与 sampleFreq 配对持久化）
    private Integer urgent;        // 是否紧急（0-否，1-是）
    private String submitBy;
    private String sourceName;     // 来源名称（字典 value，与 source 配对持久化）
    private String createBy;       // 创建人-工号（username）
    private String createName;     // 创建人-姓名（realName）
    private String updateBy;       // 更新人-工号（username）
    private String updateName;     // 更新人-姓名（realName）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
