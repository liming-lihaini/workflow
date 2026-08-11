package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 资源管理 - 危化品详情 VO：
 * 台账基本信息 + 关联流程（报废申请 ZCBFSQ 流程实例）。
 */
@Data
public class EmsHazardousDetailVO {
    private Long id;
    private String name;
    private String casNo;
    private String category;
    private String qty;
    private String unit;
    private String status;
    private String applyBy;
    private String applyReason;
    private String applyTime;
    private String remark;
    private String createTime;
    private List<EmsResourceDetailVO.RelatedProcess> relatedProcesses;
}
