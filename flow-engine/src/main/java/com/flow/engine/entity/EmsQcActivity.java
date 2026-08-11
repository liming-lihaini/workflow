package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private String operatorId; // 活动执行人（用户账号）
    private String operatorName; // 活动执行人姓名
    private String taskNo; // 任务编号：T+yyyyMMdd+4位序号，如 T202608080001
    private String createdBy;    // 创建人账号
    private String createdName;  // 创建人名称
    private LocalDate actDate;
    private LocalDate startDate;  // 开始日期
    private LocalDate endDate;    // 结束日期
    private String description;   // 活动描述（富文本HTML）
    private String taskStatus;    // 任务状态（字典 moni_qc_task_status）
    private String remark;
    private LocalDate createTime;
    private LocalDate updateTime;

    /** 所属计划名称（非表字段，待办列表展示用） */
    @TableField(exist = false)
    private String planTitle;
}
