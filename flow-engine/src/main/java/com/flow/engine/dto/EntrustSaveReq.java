package com.flow.engine.dto;

import com.flow.engine.entity.EmsEntrust;
import com.flow.engine.entity.EmsMonitorPoint;
import lombok.Data;

import java.util.List;

/**
 * 委托保存请求：委托主体 + 监测点位列表（委托基础信息）
 */
@Data
public class EntrustSaveReq {
    private EmsEntrust entrust;
    private List<EmsMonitorPoint> points;
}
