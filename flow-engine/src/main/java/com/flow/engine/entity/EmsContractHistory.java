package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同管理台账 - 操作历史：记录合同的新建、编辑、提交、中止、恢复、作废
 * 及收/付款登记与撤销轨迹，供合同详情页「操作记录」展示（参照委托操作历史模式）
 */
@Data
@TableName("t_contract_history")
public class EmsContractHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;    // 合同ID
    private String action;      // 操作动作：新建/编辑/提交/中止/恢复/作废/收款登记/支付登记/撤销登记
    private String content;     // 操作内容说明
    private String operatorId;  // 操作人账号
    private String operatorName;// 操作人姓名
    private LocalDateTime createTime;
}
