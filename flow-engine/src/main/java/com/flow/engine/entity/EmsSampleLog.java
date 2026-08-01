package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 样品操作流转日志（TRD 5.3 全程留痕）
 */
@Data
@TableName("t_sample_log")
public class EmsSampleLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sampleId;   // 样品ID
    private String action;   // 收样/留样/送检/处置
    private String operator; // 操作人
    private String detail;   // 操作详情
    private LocalDateTime createTime;
}
