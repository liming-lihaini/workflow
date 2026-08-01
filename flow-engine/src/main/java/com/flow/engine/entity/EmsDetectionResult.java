package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 检测结果明细（ISSUE-025）
 * 每个监测项目一条记录，关联检测任务。
 */
@Data
@TableName("t_detection_result")
public class EmsDetectionResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;        // 检测任务ID
    private Long sampleId;      // 样品ID
    private String monitorItem; // 监测项目（pH/COD/氨氮等）
    private String value;       // 检测值
    private String unit;        // 单位
    private String method;      // 检测方法/标准
    private String limitValue;  // 标准限值
    private String conclusion;  // 达标/超标
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
