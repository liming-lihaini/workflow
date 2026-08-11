package com.flow.engine.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收/付款登记请求（PRD-02 §3/§4）：流水信息 + 节点核销分摊
 */
@Data
public class ContractTxnReq {
    private Long contractId;
    private String txnDate;      // yyyy-MM-dd
    private BigDecimal amount;   // > 0
    private String payMethod;    // 字典 pay_method
    private String txnNo;
    private String remark;
    /** 节点核销分摊，合计必须等于 amount */
    private List<Alloc> allocations;

    @Data
    public static class Alloc {
        private Long nodeId;
        private BigDecimal amount;
    }
}
