package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 委托明细（TRD 5.1 监测项目/执行标准/频次/样品要求/限值）
 */
@Data
@TableName("t_entrust_detail")
public class EmsEntrustDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long entrustId;
    private String item;        // 监测项目
    private Long standardId;    // 执行标准方法ID（引用 t_standard，预留）
    private String freq;        // 频次
    private String sampleReq;   // 样品要求
    private String limitVal;    // 限值
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
