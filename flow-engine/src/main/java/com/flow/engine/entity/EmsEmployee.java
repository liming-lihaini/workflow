package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 环境监测 - 采样人员（ISSUE-023 派单资源；030 人员体系基础，复用 EmsVehicle 极简结构）
 */
@Data
@TableName("t_employee")
public class EmsEmployee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String phone;
    private Long qualFileId;   // 资质文件（复用 EmsCustomer.qualFileId 约定）
    private LocalDate qualDue; // 资质有效期（闸门校验）
    private Integer status;    // 1-在职 0-离职
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
