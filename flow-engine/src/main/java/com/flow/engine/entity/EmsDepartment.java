package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 组织机构/部门（TRD 5.10）
 */
@Data
@TableName("t_department")
public class EmsDepartment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deptNo;
    private String deptName;
    private Long parentId;
    private Long leaderId;
    private String leaderName;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
