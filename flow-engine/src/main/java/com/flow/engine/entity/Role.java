package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体（ISSUE-013）
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色Key */
    private String roleKey;

    /** 角色名称 */
    private String roleName;

    /** 角色类型：1-系统角色，2-业务角色 */
    private Integer roleType;

    /** 父角色ID */
    private Long parentId;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0-停用，1-正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
