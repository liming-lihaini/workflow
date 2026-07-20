package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 数据权限实体（ISSUE-013）
 */
@Data
@TableName("sys_data_permission")
public class DataPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色ID */
    private Long roleId;

    /** 部门ID */
    private Long deptId;

    /** 数据范围：1-全部，2-本部门，3-本部门及子部门，4-仅本人 */
    private Integer dataScope;
}
