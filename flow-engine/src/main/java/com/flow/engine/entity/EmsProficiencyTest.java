package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 能力验证（ISSUE-026 / TRD 5.12 G6） */
@Data
@TableName("t_proficiency_test")
public class EmsProficiencyTest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String org;
    private String item;
    private Long standardId;
    private String result;
    private String conclusion; // 合格/不合格
    private String certFile;
    private String employeeIds; // JSON
    private LocalDate testDate;
    private String remark;
    private LocalDate createTime;
    private LocalDate updateTime;
}
