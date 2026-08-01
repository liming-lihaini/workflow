package com.flow.engine.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsAlert;
import com.flow.engine.mapper.EmsAlertMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 环境监测 - 预警服务（TRD 4.4 共享实体）
 */
@Service
public class EmsAlertService extends ServiceImpl<EmsAlertMapper, EmsAlert> {

    public EmsAlert push(EmsAlert a) {
        a.setStatus(a.getStatus() == null ? 0 : a.getStatus());
        a.setCreateTime(LocalDateTime.now());
        this.save(a);
        return a;
    }
}
