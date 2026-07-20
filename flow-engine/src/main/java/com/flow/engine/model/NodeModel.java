package com.flow.engine.model;

import lombok.Data;

import java.util.Map;

/**
 * 节点模型
 */
@Data
public class NodeModel {

    /** 节点ID（流程内唯一） */
    private String id;

    /** 节点类型：start / end / userTask / serviceTask / exclusiveGateway 等 */
    private String type;

    /** 节点名称 */
    private String name;

    /** 处理人（userTask 适用） */
    private String assignee;

    /** 候选人（逗号分隔） */
    private String candidateUsers;

    /** 节点扩展属性 */
    private Map<String, Object> properties;

    /** 位置 X（前端设计器用） */
    private Integer x;

    /** 位置 Y（前端设计器用） */
    private Integer y;
}
