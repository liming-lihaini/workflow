package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 实验室间比对（ISSUE-026 / TRD 5.12 G6） */
@Data
@TableName("t_interlab_compare")
public class EmsInterlabCompare {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String partnerLab;
    private String item;
    private Long standardId;
    private String ourValue;
    private String refValue;
    private String deviation;
    private String conclusion; // 合格/不合格
    private LocalDate compareDate;
    private String remark;
    private LocalDate createTime;
    private LocalDate updateTime;
}
