package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 质控计划（ISSUE-026 / TRD 5.12 状态机） */
@Data
@TableName("t_qc_plan")
public class EmsQcPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String planNo;     // QC前缀
    private String title;
    private Integer year;
    private String quarter;    // Q1-Q4/专项
    private String type;       // 年度/季度/专项
    private String responsibleId;
    private String status;     // 草稿/审批中/执行中/已完成
    private String approvedBy;
    private LocalDate approvedAt;
    private String remark;
    private LocalDate createTime;
    private LocalDate updateTime;
}
