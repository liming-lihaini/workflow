package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 采样车辆（TRD 4.4 共享实体）
 */
@Data
@TableName("t_vehicle")
public class EmsVehicle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String plateNo;
    private String model;
    private Integer status;        // 1-可用 2-占用 3-维修（字典 moni_vehicle_status）
    private String remark;         // 备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
