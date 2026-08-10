package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人员资质信息（一个用户可持有多条资质）
 */
@Data
@TableName("sys_user_qualification")
public class UserQualification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 资质名称 */
    private String qualName;

    /** 证书编号 */
    private String certNo;

    /** 颁发机构 */
    private String issuer;

    /** 过期时间（yyyy-MM-dd） */
    private String expireDate;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
