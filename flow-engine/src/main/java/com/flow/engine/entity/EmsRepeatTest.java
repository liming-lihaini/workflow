package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 重复性试验（ISSUE-026 / TRD 5.12 G6） */
@Data
@TableName("t_repeat_test")
public class EmsRepeatTest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String item;
    private Long standardId;
    private String firstValue;
    private String repeatValue;
    private String deviation;
    private String conclusion; // 合格/不合格
    private String operatorId;
    private LocalDate testDate;
    private String remark;
    private LocalDate createTime;
    private LocalDate updateTime;
}
