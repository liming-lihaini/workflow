package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同管理台账 - 合同主表（PRD-02 §1）
 * 状态机：草稿 → 执行中 → 已完结；执行中可中止/恢复；草稿/执行中可作废
 */
@Data
@TableName("t_contract")
public class EmsContract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractNo;        // 合同编号（HT + yyyyMMdd + 4位流水，唯一）
    private String contractName;      // 合同名称
    private String contractType;      // 合同类型：收入合同 / 支出合同（dictCode: contract_type）
    private Long counterpartyId;      // 相对方ID（收入合同关联客户；支出合同为空）
    private String counterpartyName;  // 相对方名称（客户名称或供应商名称）
    private BigDecimal amount;        // 合同金额(元)
    private String signDate;          // 签订日期（yyyy-MM-dd）
    private String effectDate;        // 生效日期（yyyy-MM-dd，默认同签订日期）
    private String expireDate;        // 到期日期（yyyy-MM-dd）
    private String payMode;           // 付款方式：一次性 / 分期（dictCode: contract_pay_mode）
    private Long leadId;              // 负责人用户ID
    private String leadName;          // 负责人姓名
    private String status;            // 草稿/执行中/已完结/已中止/已作废
    private String description;       // 合同说明
    private String remark;            // 备注
    private String createBy;          // 创建人-工号
    private String createName;        // 创建人-姓名
    private String updateBy;          // 更新人-工号
    private String updateName;        // 更新人-姓名
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
