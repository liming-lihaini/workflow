package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 预警（TRD 4.4 共享实体）
 */
@Data
@TableName("t_alert")
public class EmsAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String alertType;
    private Long bizId;
    private String level;
    private String msg;
    private Integer status;
    private LocalDateTime createTime;
}
