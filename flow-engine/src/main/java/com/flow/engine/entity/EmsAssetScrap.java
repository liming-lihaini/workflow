package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 资产报废流水（设备/标准物质/耗材/危化品报废申请 ZCBFSQ 审批通过后写入）。
 * process_instance_id 作为 Webhook 幂等键，防止重试重复报废。
 */
@Data
@TableName("t_asset_scrap")
public class EmsAssetScrap {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String assetType;        // 设备/标准物质/耗材/危化品
    private Long assetId;            // 对应台账记录ID
    private String name;             // 资产名称
    private String spec;             // 规格/编号
    private String scrapReason;      // 报废原因
    private String disposeMethod;    // 处置方式
    private String applicant;        // 申请人
    private Long processInstanceId;  // 报废流程实例ID（幂等键）
    private LocalDate createTime;
}
