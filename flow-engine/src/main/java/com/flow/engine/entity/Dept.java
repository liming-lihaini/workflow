package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门实体（ISSUE-013）
 */
@Data
@TableName("sys_dept")
public class Dept {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父部门ID */
    private Long parentId;

    /** 部门名称 */
    private String deptName;

    /** 部门编码 */
    private String deptCode;

    /** 部门类型 */
    private String deptType;

    /** 排序 */
    private Integer sortOrder;

    /** 领导ID */
    private Long leaderId;

    /** 领导姓名 */
    private String leaderName;

    /** 联系电话 */
    private String phone;

    /** 状态：0-停用，1-正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
