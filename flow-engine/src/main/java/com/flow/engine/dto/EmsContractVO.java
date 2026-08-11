package com.flow.engine.dto;

import com.flow.engine.entity.EmsContractHistory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同管理台账视图对象（PRD-02）：合同基础信息 + 计算指标 +
 * 收付款节点 / 流水 / 关联委托 / 操作历史（详情时填充）
 */
@Data
public class EmsContractVO {
    private Long id;
    private String contractNo;
    private String contractName;
    private String contractType;
    private Long counterpartyId;
    private String counterpartyName;
    private BigDecimal amount;
    private String signDate;
    private String effectDate;
    private String expireDate;
    private String payMode;
    private Long leadId;
    private String leadName;
    private String status;
    private String description;
    private String remark;
    private String createBy;
    private String createName;
    private String updateBy;
    private String updateName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 已收/已付金额（流水合计） */
    private BigDecimal settledAmount;
    /** 收付进度（0-100） */
    private BigDecimal progress;
    /** 逾期未收/未付节点数 */
    private Integer overdueNodeCount;

    private List<NodeVO> nodes;
    private List<TxnVO> txns;
    private List<EntrustRefVO> entrusts;
    private List<EmsContractHistory> histories;

    @Data
    public static class NodeVO {
        private Long id;
        private Integer seq;
        private String nodeName;
        private BigDecimal planAmount;
        private String planDate;
        private String nodeDesc;
        private String status;
        /** 已核销金额（分摊合计） */
        private BigDecimal allocatedAmount;
        /** 是否逾期（计划日期 < 今天 且未收讫/付讫） */
        private Boolean overdue;
    }

    @Data
    public static class TxnVO {
        private Long id;
        private String txnType;
        private String txnDate;
        private BigDecimal amount;
        private String payMethod;
        private String txnNo;
        private String operatorId;
        private String operatorName;
        private String remark;
        private LocalDateTime createTime;
        private List<AllocVO> allocations;
    }

    @Data
    public static class AllocVO {
        private Long nodeId;
        private Integer nodeSeq;
        private String nodeName;
        private BigDecimal allocateAmount;
    }

    @Data
    public static class EntrustRefVO {
        private Long entrustId;
        private String entrustNo;
        private String entrustName;
        private String status;
    }
}
