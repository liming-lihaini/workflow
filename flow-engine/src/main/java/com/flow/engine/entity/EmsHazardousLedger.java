package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 危化品台账（ISSUE-026 / TRD 5.5 审批状态机） */
@Data
@TableName("t_hazardous_ledger")
public class EmsHazardousLedger {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String casNo;
    private String category;   // 易燃/腐蚀/有毒/易爆
    private String qty;
    private String unit;
    private String status;     // 在库/待审批/已领用/已报废
    private String applyBy;
    private String approveBy;
    private String applyReason;
    private String approveOpinion;
    private LocalDate applyTime;
    private LocalDate approveTime;
    private String remark;
    private LocalDate createTime;
    private LocalDate updateTime;
}
