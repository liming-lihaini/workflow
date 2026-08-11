package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private String createdBy;    // 创建人账号
    private String createdName;  // 创建人名称
    private LocalDate createTime;
    private LocalDate updateTime;

    /** 任务进度（非表字段）：计划下监控活动总数 */
    @TableField(exist = false)
    private Integer taskTotal;
    /** 任务进度（非表字段）：已完成（taskStatus=已完成）的监控活动数 */
    @TableField(exist = false)
    private Integer taskDone;
}
