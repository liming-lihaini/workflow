package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 物资（标准物质/耗材）入库/使用流水。
 * 由 WZRKSQ（入库申请）/ WZSYSQ（使用申请）流程审批通过后的 Webhook 写入；
 * process_instance_id 作为幂等键，避免 Webhook 重试产生重复流水，
 * 同时作为物资详情「关联流程」的数据来源。
 */
@Data
@TableName("t_material_flow")
public class EmsMaterialFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;           // 入库/出库
    private String materialType;      // 标准物质/耗材
    private Long materialId;          // 关联物资ID（t_standard_material / t_consumable）
    private String name;
    private String spec;
    private String lotNo;
    private Integer qty;              // 入库为正数；出库为正数（领用数量）
    private String applicant;
    private Long processInstanceId;
    private String remark;
    private LocalDate createTime;
}
