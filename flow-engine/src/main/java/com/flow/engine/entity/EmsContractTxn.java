package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同管理台账 - 收付款流水（PRD-02 §3/§4）
 * txnType：收款（收入合同）/ 支付（支出合同）
 */
@Data
@TableName("t_contract_txn")
public class EmsContractTxn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;        // 合同ID
    private String txnType;         // 收款 / 支付
    private String txnDate;         // 收/付款日期（yyyy-MM-dd）
    private BigDecimal amount;      // 金额(元)
    private String payMethod;       // 收/付款方式（dictCode: pay_method）
    private String txnNo;           // 交易流水号（银行回单号）
    private String operatorId;      // 经办人账号
    private String operatorName;    // 经办人姓名
    private String remark;          // 备注
    private LocalDateTime createTime;
}
