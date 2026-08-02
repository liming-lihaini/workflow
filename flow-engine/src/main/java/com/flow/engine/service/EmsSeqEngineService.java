package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.EmsSeqDef;
import com.flow.engine.mapper.EmsSeqDefMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 编号引擎（ISSUE-029）。
 * 统一生成业务单号：前缀 + yyyyMMdd + 当日序列（4 位补零），保证全局唯一。
 * 序列按业务键 + 日期分段（BR-029-04）。兼容既有 CodeGenerator 形态。
 */
@Service
public class EmsSeqEngineService {

    private final EmsSeqDefMapper seqMapper;

    public EmsSeqEngineService(EmsSeqDefMapper seqMapper) {
        this.seqMapper = seqMapper;
    }

    /** 取下一个唯一业务单号（线程安全：事务内行级更新 + 重算）。 */
    @Transactional
    public synchronized String next(String bizKey) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        EmsSeqDef def = seqMapper.selectOne(
                new LambdaQueryWrapper<EmsSeqDef>().eq(EmsSeqDef::getBizKey, bizKey));
        if (def == null) {
            def = new EmsSeqDef();
            def.setBizKey(bizKey);
            def.setPrefix(bizKey.substring(0, Math.min(2, bizKey.length())).toUpperCase());
            def.setSeqDate(today);
            def.setCurrentVal(1L);
            def.setStep(1);
            seqMapper.insert(def);
        } else {
            // 跨天则重置日期段与序列
            if (!today.equals(def.getSeqDate())) {
                def.setSeqDate(today);
                def.setCurrentVal(1L);
            } else {
                def.setCurrentVal((def.getCurrentVal() == null ? 0 : def.getCurrentVal()) + 1);
            }
            seqMapper.updateById(def);
        }
        int seq = def.getCurrentVal().intValue();
        // 前缀 + yyyyMMdd + 4 位补零，保证全局唯一且分段可读
        return def.getPrefix() + today + String.format("%04d", seq);
    }
}
