package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 检测任务（ISSUE-025）
 * 每个已收样样品生成一个检测任务，录入员填报监测项目结果后提交复核。
 * 状态机：待录入 → 录入中 → 已提交 → 已复核 / 已退回
 */
@Data
@TableName("t_detection_task")
public class EmsDetectionTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;       // 任务编号（DT前缀）
    private Long sampleId;       // 样品ID
    private String barcode;      // 样品条码
    private String sampleName;   // 样品名称
    private Long pointId;        // 监测点位ID
    private String monitorItems; // 监测项目（逗号分隔）
    private String entryBy;      // 录入员
    private String entryTime;    // 录入时间
    private String status;       // 待录入/录入中/已提交/已复核/已退回
    private String reviewBy;     // 复核人
    private String reviewTime;   // 复核时间
    private String reviewOpinion;// 复核意见
    private String envTemp;      // 检测环境温度
    private String envHumidity;  // 检测环境湿度
    private String conclusion;   // 综合检验结论：pending/ok/ng/abnormal
    private String remark;
    private String attachments;   // 检测录入附件（JSON 数组：[{name,path}]）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
