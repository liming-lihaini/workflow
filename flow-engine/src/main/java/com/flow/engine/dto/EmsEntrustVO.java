package com.flow.engine.dto;

import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsMonitorPoint;
import lombok.Data;

import java.util.List;

/**
 * 委托单视图对象：关联客户名称、来源名称、监测点位列表
 */
@Data
public class EmsEntrustVO {
    private Long id;
    private String entrustNo;
    private Long custId;
    private String custName;       // 客户名称（关联 t_customer）
    private String entrustName;
    private String source;         // 来源编码
    private String sourceName;     // 来源名称（关联字典 moni_entrust_source）
    private String status;
    private String description;    // 委托说明（富文本）
    private String submitBy;
    private String createTime;
    private String updateTime;
    private List<EmsMonitorPoint> points;  // 监测点位（委托基础信息）

    public static EmsEntrustVO from(EmsEntrust e) {
        EmsEntrustVO vo = new EmsEntrustVO();
        vo.setId(e.getId());
        vo.setEntrustNo(e.getEntrustNo());
        vo.setCustId(e.getCustId());
        vo.setEntrustName(e.getEntrustName());
        vo.setSource(e.getSource());
        vo.setStatus(e.getStatus());
        vo.setDescription(e.getDescription());
        vo.setSubmitBy(e.getSubmitBy());
        vo.setCreateTime(e.getCreateTime() == null ? null : e.getCreateTime().toString());
        vo.setUpdateTime(e.getUpdateTime() == null ? null : e.getUpdateTime().toString());
        return vo;
    }
}
