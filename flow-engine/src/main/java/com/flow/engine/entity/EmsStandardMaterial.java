package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 标准物质（ISSUE-026 / TRD 5.5） */
@Data
@TableName("t_standard_material")
public class EmsStandardMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String lotNo;
    private String spec;
    private LocalDate expireDate;
    private Integer stock;
    private String status;     // 在库/临期/过期
    private String certNo;
    private String remark;
    private String createBy;
    private LocalDate createTime;
    private LocalDate updateTime;
}
