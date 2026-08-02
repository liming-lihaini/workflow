package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 质量控制监控活动（ISSUE-026 / TRD 5.12：空白/平行/加标回收/留样复测） */
@Data
@TableName("t_qc_activity")
public class EmsQcActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String qcType;     // 空白/平行/加标回收/留样复测
    private String item;
    private Long standardId;
    private Long batchId;      // 关联检测批次（可空，联动025）
    private String result;
    private String passFlag;   // 合格/不合格
    private String operatorId;
    private LocalDate actDate;
    private String remark;
    private LocalDate createTime;
    private LocalDate updateTime;
}
