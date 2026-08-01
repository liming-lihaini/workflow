package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsVehicle;
import com.flow.engine.mapper.EmsVehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 环境监测 - 采样车辆服务（TRD 4.4 共享实体 / 车辆台账）
 */
@Service
public class EmsVehicleService extends ServiceImpl<EmsVehicleMapper, EmsVehicle> {

    public EmsVehicle create(EmsVehicle v) {
        if (!StringUtils.hasText(v.getPlateNo())) {
            throw new IllegalArgumentException("车牌号不能为空");
        }
        v.setStatus(v.getStatus() == null ? 1 : v.getStatus());
        v.setCreateTime(LocalDateTime.now());
        v.setUpdateTime(LocalDateTime.now());
        this.save(v);
        return v;
    }

    public EmsVehicle updateVehicle(Long id, EmsVehicle v) {
        EmsVehicle exist = getById(id);
        if (exist == null) throw new IllegalArgumentException("车辆不存在: " + id);
        v.setId(id);
        v.setUpdateTime(LocalDateTime.now());
        this.updateById(v);
        return v;
    }

    /** 分页检索（支持车牌/型号关键字） */
    public Page<EmsVehicle> pageSearch(String keyword, int page, int size) {
        LambdaQueryWrapper<EmsVehicle> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            q.like(EmsVehicle::getPlateNo, keyword).or().like(EmsVehicle::getModel, keyword);
        }
        q.orderByDesc(EmsVehicle::getCreateTime);
        return this.page(new Page<>(page, size), q);
    }
}
