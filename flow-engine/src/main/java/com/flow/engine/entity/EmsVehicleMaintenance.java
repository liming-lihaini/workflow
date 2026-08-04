package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 车辆维修保养记录（ISSUE-036）
 */
@Data
@TableName("t_vehicle_maintenance")
public class EmsVehicleMaintenance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vehicleId;
    private String maintType;     // 操作类型（字典 moni_vehicle_maint_type 的 item_value）
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
