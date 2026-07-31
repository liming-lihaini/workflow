package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人API Token实体（支持APItoken方式鉴权）
 */
@Data
@TableName("sys_api_token")
public class ApiToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** Token名称（用途备注） */
    private String tokenName;

    /** Token值 */
    private String tokenValue;

    /** 过期时间（为空则永久有效） */
    private LocalDateTime expireTime;

    /** 最近使用时间 */
    private LocalDateTime lastUsedTime;

    private LocalDateTime createTime;
}
