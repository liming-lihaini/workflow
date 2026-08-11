package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 资源管理 - 物资（标准物质/耗材）详情 VO：
 * 台账基本信息 + 关联流程（入库申请 WZRKSQ / 使用申请 WZSYSQ 流程实例）。
 */
@Data
public class EmsResourceDetailVO {
    private Long id;
    private String type;            // 标准物质 / 耗材
    private String name;
    private String spec;
    private String lotNo;           // 标准物质批号
    private String certNo;          // 标准物质证书编号
    private String expireDate;
    private Integer stock;          // 标准物质=库存(stock)，耗材=数量(qty)
    private String status;
    private String remark;
    private String createBy;
    private String createTime;
    private List<RelatedProcess> relatedProcesses;

    @Data
    public static class RelatedProcess {
        private Long processInstanceId;
        private String instanceNo;
        private String processKey;
        private String processName;
        private String statusText;
        private String startUser;
        private String startTime;
        private String bizType;     // 入库/出库（来自流水记录，流程发起中为空）
        private Integer qty;        // 该流程涉及的入库/使用数量
    }
}
