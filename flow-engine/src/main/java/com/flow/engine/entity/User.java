package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（ISSUE-013）
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 主部门ID */
    private Long deptId;

    /** 主岗位ID */
    private Long postId;

    /** 密级：1-公开，2-内部，3-秘密，4-机密 */
    private Integer securityLevel;

    /** 状态：0-停用，1-正常，2-锁定 */
    private Integer status;

    /** 性别：男/女 */
    private String gender;

    /** 出生年月（yyyy-MM） */
    private String birthDate;

    /** 用户头像附件相对路径（yyyyMMdd/uuid.ext，经 /attachments/preview 展示） */
    private String avatar;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 人员资质列表（非持久化字段，创建/更新用户时随请求传入） */
    @TableField(exist = false)
    private java.util.List<UserQualification> qualifications;
}
