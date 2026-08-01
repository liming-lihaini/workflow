package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsInstrumentCalib;
import com.flow.engine.mapper.EmsInstrumentCalibMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmsInstrumentCalibService extends ServiceImpl<EmsInstrumentCalibMapper, EmsInstrumentCalib> {

    /** 按设备查询校准历史，时间倒序 */
    public List<EmsInstrumentCalib> listByInstrument(Long instrumentId) {
        return this.list(new LambdaQueryWrapper<EmsInstrumentCalib>()
                .eq(EmsInstrumentCalib::getInstrumentId, instrumentId)
                .orderByDesc(EmsInstrumentCalib::getCreateTime));
    }

    /** 写入一条校准历史 */
    public void record(Long instrumentId, java.time.LocalDate calibDate, java.time.LocalDate calibDue, String certNo) {
        EmsInstrumentCalib rec = new EmsInstrumentCalib();
        rec.setInstrumentId(instrumentId);
        rec.setCalibDate(calibDate);
        rec.setCalibDue(calibDue);
        rec.setCertNo(certNo);
        rec.setCreateTime(LocalDateTime.now());
        this.save(rec);
    }
}
