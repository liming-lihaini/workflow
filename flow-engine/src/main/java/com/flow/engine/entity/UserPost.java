package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 用户兼职实体（ISSUE-013）
 */
@Data
@TableName("sys_user_post")
public class UserPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 岗位ID */
    private Long postId;

    /** 是否主部门：0-兼职，1-主部门 */
    private Integer isMain;
}
