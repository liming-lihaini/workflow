package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同管理台账 - 收付款节点（PRD-02 §2）
 * 收入合同为收款节点（应收计划），支出合同为付款节点（应付计划）
 */
@Data
@TableName("t_contract_node")
public class EmsContractNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;        // 合同ID
    private Integer seq;            // 期数（1、2、3…）
    private String nodeName;        // 节点名称（预付款/进度款/验收款/质保金等）
    private BigDecimal planAmount;  // 计划金额(元)，Σ节点金额 = 合同金额
    private String planDate;        // 计划收/付款日期（yyyy-MM-dd），用于逾期判定
    private String nodeDesc;        // 节点说明（付款条件等）
    private String status;          // 待收/待付 → 部分收/部分付 → 已收讫/已付讫（核销重算）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
