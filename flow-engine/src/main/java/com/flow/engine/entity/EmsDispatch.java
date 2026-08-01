package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 调度派单主表（TRD 5.2 派单动作内嵌于 待派单→已派单）
 */
@Data
@TableName("t_dispatch")
public class EmsDispatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;       // 订单ID
    private String status;      // 待派单/已派单/采样执行中/样品送检
    private LocalDateTime dispatchTime;
    private LocalDateTime planStart;
    private LocalDateTime planEnd;
    private Long vehicleId;     // 车辆ID
    private String note;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
