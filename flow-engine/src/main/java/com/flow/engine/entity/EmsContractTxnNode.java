package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同管理台账 - 流水与节点核销分摊（PRD-02 §3.2）
 * 一笔流水可分摊核销多个节点，分摊合计 = 流水金额
 */
@Data
@TableName("t_contract_txn_node")
public class EmsContractTxnNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long txnId;              // 流水ID（t_contract_txn.id）
    private Long nodeId;             // 节点ID（t_contract_node.id）
    private BigDecimal allocateAmount; // 分摊金额(元)
}
