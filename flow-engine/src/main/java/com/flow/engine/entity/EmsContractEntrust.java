package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 合同管理台账 - 合同与检测委托关联（PRD-02 §6，收入合同多对多）
 */
@Data
@TableName("t_contract_entrust")
public class EmsContractEntrust {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;  // 合同ID
    private Long entrustId;   // 检测委托ID（t_entrust.id）
}
