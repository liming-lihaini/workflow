package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 复核记录（ISSUE-025）
 * 每次复核留痕，支持多次退回/再提交。
 */
@Data
@TableName("t_detection_review")
public class EmsDetectionReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;       // 检测任务ID
    private Long sampleId;     // 样品ID
    private String barcode;    // 样品条码
    private String reviewer;   // 复核人
    private String decision;   // 通过/退回
    private String opinion;    // 复核意见
    private LocalDateTime createTime;
}
