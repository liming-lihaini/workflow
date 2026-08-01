package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 技术确认记录（BR-023-01 技术室评审意见）
 */
@Data
@TableName("t_entrust_review")
public class EmsEntrustReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long entrustId;
    private Long reviewerId;    // 技术室ID
    private String opinion;     // 确认意见
    private String result;      // PASS-通过 / REJECT-退回
    private LocalDateTime reviewAt;
}
