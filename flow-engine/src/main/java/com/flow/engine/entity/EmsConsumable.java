package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 耗材（ISSUE-026 / TRD 5.5） */
@Data
@TableName("t_consumable")
public class EmsConsumable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String spec;
    private Integer qty;
    private LocalDate expireDate;
    private String status;     // 在库/临期/过期
    private String remark;
    private String createBy;
    private LocalDate createTime;
    private LocalDate updateTime;
}
