package com.flow.engine.dto;

import com.flow.engine.entity.EmsContract;
import com.flow.engine.entity.EmsContractNode;
import lombok.Data;

import java.util.List;

/**
 * 合同保存请求：合同基础信息 + 收付款节点（整体替换）+ 关联委托ID
 */
@Data
public class ContractSaveReq {
    private EmsContract contract;
    private List<EmsContractNode> nodes;
    private List<Long> entrustIds;
}
