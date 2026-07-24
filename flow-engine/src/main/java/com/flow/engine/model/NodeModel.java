package com.flow.engine.model;

import lombok.Data;

import java.util.List;
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

    /** 处理人分配方式：user-指定用户, dept_leader-部门领导 */
    private String assigneeType;

    /** 部门ID（assigneeType=dept_leader 时使用） */
    private Long assigneeDeptId;

    /** 节点扩展属性 */
    private Map<String, Object> properties;

    /** 节点事件定义（beforeEnter/afterEnter/afterComplete/afterReject） */
    private List<Map<String, Object>> events;

    /** 位置 X（前端设计器用） */
    private Integer x;

    /** 位置 Y（前端设计器用） */
    private Integer y;
}
