package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsMonitorPoint;
import com.flow.engine.mapper.EmsMonitorPointMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境监测 - 监测点位服务（TRD 5.1）
 * 业务规则：BR-022-03 点位名必填；BR-022-04 经纬度范围校验；BR-022-06 删除前需先解除与客户的关联（由引用方校验）
 */
@Service
public class EmsMonitorPointService extends ServiceImpl<EmsMonitorPointMapper, EmsMonitorPoint> {

    public EmsMonitorPoint create(EmsMonitorPoint p) {
        if (!StringUtils.hasText(p.getPointName())) {
            throw new IllegalArgumentException("点位名称不能为空(BR-022-03)");
        }
        if (p.getLng() != null && (p.getLng() < -180 || p.getLng() > 180)) {
            throw new IllegalArgumentException("经度超出范围[-180,180](BR-022-04)");
        }
        if (p.getLat() != null && (p.getLat() < -90 || p.getLat() > 90)) {
            throw new IllegalArgumentException("纬度超出范围[-90,90](BR-022-04)");
        }
        p.setHistoryOverFlag(p.getHistoryOverFlag() == null ? 0 : p.getHistoryOverFlag());
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        this.save(p);
        return p;
    }

    public List<EmsMonitorPoint> listByCust(Long custId) {
        return this.list(new LambdaQueryWrapper<EmsMonitorPoint>().eq(custId != null, EmsMonitorPoint::getCustId, custId));
    }

    public List<EmsMonitorPoint> listByEntrust(Long entrustId) {
        return this.list(new LambdaQueryWrapper<EmsMonitorPoint>().eq(entrustId != null, EmsMonitorPoint::getEntrustId, entrustId));
    }
}
