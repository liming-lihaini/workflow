package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 环境监测 - 采样设备/仪器（TRD 5.5 仪器设备全生命周期台账）
 * 状态机（5.5.3）：在用 → 校准临期 →（到期）强制停用 → 校准/维修 → 恢复在用 / 报废
 */
@Data
@TableName("t_instrument")
public class EmsInstrument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;         // 仪器编号（YQ前缀）
    private String name;         // 仪器名称
    private String model;        // 型号
    private String manufacturer; // 生产厂商
    private LocalDate purchaseDate;   // 购置日期
    private LocalDate calibDue;       // 校准到期日（物资闸门：过期强制停用）
    private LocalDate lastCalibDate;  // 上次校准日期
    private String certNo;           // 校准证书编号
    private String status;          // 在用/临期/停用/维修/报废（5.5.3 + F2）
    private String remark;          // 备注
    private String createBy;        // 创建人（入库审批通过时取流程申请人）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
