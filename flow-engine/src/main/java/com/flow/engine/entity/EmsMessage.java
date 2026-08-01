package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 站内信（TRD 4.4 共享实体）
 */
@Data
@TableName("t_message")
public class EmsMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String toUser;
    private String title;
    private String content;
    private Integer readFlag;
    private LocalDateTime createTime;
}
