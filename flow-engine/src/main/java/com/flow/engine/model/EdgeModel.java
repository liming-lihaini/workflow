package com.flow.engine.model;

import lombok.Data;

/**
 * 连线模型（流程中节点之间的流转关系）
 */
@Data
public class EdgeModel {

    /** 连线ID */
    private String id;

    /** 源节点ID */
    private String source;

    /** 目标节点ID */
    private String target;

    /** 连线名称/标签 */
    private String label;

    /** 条件表达式（排他网关等条件分支使用） */
    private String condition;
}
