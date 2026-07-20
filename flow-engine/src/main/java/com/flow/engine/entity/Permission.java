package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体（ISSUE-013）
 */
@Data
@TableName("sys_permission")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父权限ID */
    private Long parentId;

    /** 权限名称 */
    private String permName;

    /** 权限Key */
    private String permKey;

    /** 权限类型：1-菜单，2-按钮，3-接口 */
    private Integer permType;

    /** 资源路径 */
    private String resourcePath;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
