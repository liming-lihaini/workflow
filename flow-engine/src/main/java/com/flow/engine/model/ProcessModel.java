package com.flow.engine.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程定义解析模型（TRD §4.1.3）
 */
@Data
public class ProcessModel {

    /** 流程Key */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 分类 */
    private String category;

    /** 节点列表 */
    private List<NodeModel> nodes;

    /** 连线列表 */
    private List<EdgeModel> edges;

    /** 全局变量默认值 */
    private Map<String, Object> variables;
}
