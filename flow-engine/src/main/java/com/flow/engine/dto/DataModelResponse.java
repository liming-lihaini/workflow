package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 数据模型响应（ISSUE-010）
 */
@Data
public class DataModelResponse {

    private Long id;
    private String modelKey;
    private String modelName;
    private String modelJson;
    private Integer version;
    private Integer status;
    private DataModelRequest.TableDefinition mainTable;
    private List<DataModelRequest.TableDefinition> subTables;
    private String createTime;
    private String updateTime;
}
