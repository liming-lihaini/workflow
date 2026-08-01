package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 委托单主数据框架（TRD 5.1，完整状态机见 ISSUE-023）
 */
@Data
@TableName("t_entrust")
public class EmsEntrust {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String entrustNo;
    private Long custId;
    private String entrustName;
    private String source;
    private String status;
    private String description;  // 委托说明（富文本）
    private String submitBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
