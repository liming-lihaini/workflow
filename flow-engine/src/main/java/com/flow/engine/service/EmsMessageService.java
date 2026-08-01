package com.flow.engine.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsMessage;
import com.flow.engine.mapper.EmsMessageMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 环境监测 - 站内信服务（TRD 4.4 共享实体）
 */
@Service
public class EmsMessageService extends ServiceImpl<EmsMessageMapper, EmsMessage> {

    public EmsMessage send(EmsMessage m) {
        m.setReadFlag(m.getReadFlag() == null ? 0 : m.getReadFlag());
        m.setCreateTime(LocalDateTime.now());
        this.save(m);
        return m;
    }
}
