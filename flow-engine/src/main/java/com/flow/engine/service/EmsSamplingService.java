package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.EmsPhoto;
import com.flow.engine.entity.EmsSample;
import com.flow.engine.entity.EmsSampleLog;
import com.flow.engine.entity.EmsSampleQcBinding;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.entity.EmsRetain;
import com.flow.engine.entity.EmsSamplingRecord;
import com.flow.engine.mapper.EmsSampleMapper;
import com.flow.engine.mapper.EmsSampleQcBindingMapper;
import com.flow.engine.mapper.EmsSampleLogMapper;
import com.flow.engine.mapper.EmsSamplingRecordMapper;
import com.flow.engine.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境监测 - 采样记录 + 样品管理（TRD 5.3 / ISSUE-024）
 */
@Service
public class EmsSamplingService extends ServiceImpl<EmsSamplingRecordMapper, EmsSamplingRecord> {

    @Autowired
    private EmsSampleMapper sampleMapper;
    @Autowired
    private EmsSampleQcBindingMapper qcMapper;
    @Autowired
    private EmsSampleLogMapper sampleLogMapper;
    @Autowired
    private EmsSamplingOrderService samplingOrderService;
    @Autowired
    private EmsRetainService retainService;

    // ===================== 采样记录 =====================

    public EmsSamplingRecord createRecord(EmsSamplingRecord rec) {
        if (rec.getOrderId() == null) throw new BusinessException("采样订单ID不能为空");
        if (rec.getPointId() == null) throw new BusinessException("监测点位不能为空");
        rec.setStatus(StringUtils.hasText(rec.getStatus()) ? rec.getStatus() : "采样中");
        rec.setCreateTime(LocalDateTime.now());
        rec.setUpdateTime(LocalDateTime.now());
        this.save(rec);
        return rec;
    }

    public EmsSamplingRecord updateRecord(Long id, EmsSamplingRecord rec) {
        EmsSamplingRecord exist = getById(id);
        if (exist == null) throw new BusinessException("采样记录不存在: " + id);
        rec.setId(id);
        if (!StringUtils.hasText(rec.getStatus())) rec.setStatus(exist.getStatus());
        rec.setUpdateTime(LocalDateTime.now());
        this.updateById(rec);
        return rec;
    }

    /** 完成采样（采样中 → 采样完成），并同步采样订单状态 */
    @Transactional(rollbackFor = Exception.class)
    public EmsSamplingRecord completeRecord(Long id) {
        EmsSamplingRecord exist = getById(id);
        if (exist == null) throw new BusinessException("采样记录不存在: " + id);
        exist.setStatus("采样完成");
        exist.setUpdateTime(LocalDateTime.now());
        this.updateById(exist);
        syncOrderStatus(exist.getOrderId(), "样品送检");
        return exist;
    }

    public Page<EmsSamplingRecord> pageRecords(Long orderId, String status, String keyword, int page, int size) {
        LambdaQueryWrapper<EmsSamplingRecord> q = new LambdaQueryWrapper<>();
        if (orderId != null) q.eq(EmsSamplingRecord::getOrderId, orderId);
        if (StringUtils.hasText(status)) q.eq(EmsSamplingRecord::getStatus, status);
        if (StringUtils.hasText(keyword)) q.like(EmsSamplingRecord::getSampler, keyword);
        q.orderByDesc(EmsSamplingRecord::getCreateTime);
        return this.page(new Page<>(page, size), q);
    }

    public void deleteRecord(Long id) {
        EmsSamplingRecord exist = getById(id);
        if (exist == null) return;
        // 级联删除关联样品、照片
        sampleMapper.delete(new LambdaQueryWrapper<EmsSample>().eq(EmsSample::getSamplingId, id));
        this.removeById(id);
    }

    // ===================== 样品 =====================

    public EmsSample createSample(EmsSample sample) {
        if (sample.getSamplingId() == null) throw new BusinessException("采样记录ID不能为空");
        if (!StringUtils.hasText(sample.getName())) throw new BusinessException("样品名称不能为空");
        sample.setStatus(StringUtils.hasText(sample.getStatus()) ? sample.getStatus() : "待收样");
        // 生成条码
        int seq = (int) (sampleMapper.selectCount(new LambdaQueryWrapper<>()) + 1);
        sample.setBarcode(CodeGenerator.generate("YP", seq));
        sample.setCreateTime(LocalDateTime.now());
        sample.setUpdateTime(LocalDateTime.now());
        sampleMapper.insert(sample);
        writeLog(sample.getId(), "创建", null, "新建样品: " + sample.getName());
        return sample;
    }

    public EmsSample updateSample(Long id, EmsSample sample) {
        EmsSample exist = sampleMapper.selectById(id);
        if (exist == null) throw new BusinessException("样品不存在: " + id);
        sample.setId(id);
        if (!StringUtils.hasText(sample.getStatus())) sample.setStatus(exist.getStatus());
        sample.setUpdateTime(LocalDateTime.now());
        sampleMapper.updateById(sample);
        return sample;
    }

    /** 收样：样品 待收样 → 已收样，记录收样人与收样时间 */
    @Transactional(rollbackFor = Exception.class)
    public EmsSample receive(Long id, String receiveBy, String receiveTime, String remark) {
        EmsSample exist = sampleMapper.selectById(id);
        if (exist == null) throw new BusinessException("样品不存在: " + id);
        if (!"待收样".equals(exist.getStatus())) throw new BusinessException("仅待收样状态可登记收样");
        exist.setStatus("已收样");
        exist.setReceiveBy(receiveBy);
        exist.setReceiveTime(receiveTime == null ? LocalDate.now().toString() : receiveTime);
        if (StringUtils.hasText(remark)) exist.setRemark(remark);
        exist.setUpdateTime(LocalDateTime.now());
        sampleMapper.updateById(exist);
        writeLog(id, "收样", receiveBy, "收样时间: " + exist.getReceiveTime());
        return exist;
    }

    /** 送检：样品 已收样 → 实验室检测（dispatch_time 记录下发时间） */
    @Transactional(rollbackFor = Exception.class)
    public EmsSample dispatch(Long id, String dispatchTime) {
        EmsSample exist = sampleMapper.selectById(id);
        if (exist == null) throw new BusinessException("样品不存在: " + id);
        exist.setStatus("已收样");
        exist.setDispatchTime(dispatchTime == null ? LocalDate.now().toString() : dispatchTime);
        exist.setUpdateTime(LocalDateTime.now());
        sampleMapper.updateById(exist);
        writeLog(id, "送检", null, "送检时间: " + exist.getDispatchTime());
        return exist;
    }

    public Page<EmsSample> pageSamples(Long orderId, Long samplingId, String status, String keyword, int page, int size) {
        LambdaQueryWrapper<EmsSample> q = new LambdaQueryWrapper<>();
        if (orderId != null) q.eq(EmsSample::getOrderId, orderId);
        if (samplingId != null) q.eq(EmsSample::getSamplingId, samplingId);
        if (StringUtils.hasText(status)) q.eq(EmsSample::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            q.like(EmsSample::getName, keyword).or().like(EmsSample::getBarcode, keyword);
        }
        q.orderByDesc(EmsSample::getCreateTime);
        return sampleMapper.selectPage(new Page<>(page, size), q);
    }

    public List<EmsSample> listByStatus(String status) {
        LambdaQueryWrapper<EmsSample> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) q.eq(EmsSample::getStatus, status);
        return sampleMapper.selectList(q);
    }

    public void deleteSample(Long id) {
        EmsSample exist = sampleMapper.selectById(id);
        if (exist == null) return;
        qcMapper.delete(new LambdaQueryWrapper<EmsSampleQcBinding>().eq(EmsSampleQcBinding::getSampleId, id));
        sampleLogMapper.delete(new LambdaQueryWrapper<EmsSampleLog>().eq(EmsSampleLog::getSampleId, id));
        sampleMapper.deleteById(id);
    }

    // ===================== 质控样 / 照片 / 日志 =====================

    public EmsSampleQcBinding bindQc(EmsSampleQcBinding binding) {
        if (binding.getSampleId() == null) throw new BusinessException("样品ID不能为空");
        qcMapper.insert(binding);
        return binding;
    }

    public void unbindQc(Long id) {
        qcMapper.deleteById(id);
    }

    public List<EmsSampleQcBinding> listQc(Long sampleId) {
        return qcMapper.selectList(new LambdaQueryWrapper<EmsSampleQcBinding>().eq(EmsSampleQcBinding::getSampleId, sampleId));
    }

    @Autowired
    private com.flow.engine.mapper.EmsPhotoMapper photoMapperRef;

    public EmsPhoto addPhoto(EmsPhoto photo) {
        photo.setCreateTime(LocalDateTime.now());
        photoMapperRef.insert(photo);
        return photo;
    }

    public List<EmsPhoto> listPhotos(String bizType, Long bizId) {
        return photoMapperRef.selectList(new LambdaQueryWrapper<EmsPhoto>()
                .eq(EmsPhoto::getBizType, bizType).eq(EmsPhoto::getBizId, bizId));
    }

    public void writeLog(Long sampleId, String action, String operator, String detail) {
        EmsSampleLog log = new EmsSampleLog();
        log.setSampleId(sampleId);
        log.setAction(action);
        log.setOperator(operator);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        sampleLogMapper.insert(log);
    }

    public List<EmsSampleLog> listLogs(Long sampleId) {
        return sampleLogMapper.selectList(new LambdaQueryWrapper<EmsSampleLog>()
                .eq(EmsSampleLog::getSampleId, sampleId).orderByDesc(EmsSampleLog::getCreateTime));
    }

    // ===================== 留样 =====================

    /** 登记留样：样品 → 已收样/留样中，写入留样库 */
    @Transactional(rollbackFor = Exception.class)
    public EmsRetain retain(Long sampleId, Integer retainDays, String retainBy, String retainTime, String remark) {
        EmsSample sample = sampleMapper.selectById(sampleId);
        if (sample == null) throw new BusinessException("样品不存在: " + sampleId);
        if (retainDays == null || retainDays <= 0) throw new BusinessException("留样天数必须大于0");
        LocalDate start = retainTime == null ? LocalDate.now() : LocalDate.parse(retainTime);
        LocalDate until = start.plusDays(retainDays);
        EmsRetain retain = new EmsRetain();
        retain.setSampleId(sampleId);
        retain.setBarcode(sample.getBarcode());
        retain.setName(sample.getName());
        retain.setPointId(sample.getPointId());
        retain.setRetainBy(retainBy);
        retain.setRetainTime(start.toString());
        retain.setRetainDays(retainDays);
        retain.setRetainUntil(until.toString());
        retain.setStatus("留样中");
        retain.setRemark(remark);
        retain.setCreateTime(LocalDateTime.now());
        retain.setUpdateTime(LocalDateTime.now());
        retainService.save(retain);

        sample.setRetainFlag(1);
        sample.setRetainDays(retainDays);
        sample.setRetainUntil(until.toString());
        sample.setStatus("留样中");
        sample.setUpdateTime(LocalDateTime.now());
        sampleMapper.updateById(sample);
        writeLog(sampleId, "留样", retainBy, "留样至 " + until + "（" + retainDays + "天）");
        return retain;
    }

    /** 处置留样：留样库 → 已处置，样品 → 已处置 */
    @Transactional(rollbackFor = Exception.class)
    public EmsRetain dispose(Long retainId, String disposeBy, String disposeTime) {
        EmsRetain retain = retainService.getById(retainId);
        if (retain == null) throw new BusinessException("留样记录不存在: " + retainId);
        retain.setStatus("已处置");
        retain.setDisposeBy(disposeBy);
        retain.setDisposeTime(disposeTime == null ? LocalDate.now().toString() : disposeTime);
        retain.setUpdateTime(LocalDateTime.now());
        retainService.updateById(retain);

        if (retain.getSampleId() != null) {
            EmsSample sample = sampleMapper.selectById(retain.getSampleId());
            if (sample != null) {
                sample.setStatus("已处置");
                sample.setUpdateTime(LocalDateTime.now());
                sampleMapper.updateById(sample);
                writeLog(retain.getSampleId(), "处置", disposeBy, "留样处置");
            }
        }
        return retain;
    }

    /** 留样到期预警列表（retain_until <= now+threshold） */
    public List<EmsRetain> expiringRetain(int thresholdDays) {
        LocalDate line = LocalDate.now().plusDays(thresholdDays);
        return retainService.list(new LambdaQueryWrapper<EmsRetain>()
                .eq(EmsRetain::getStatus, "留样中")
                .le(EmsRetain::getRetainUntil, line));
    }

    // ===================== 聚合 =====================

    /** 收样工作台：返回待收样样品 + 已完成采样记录，便于一键收样 */
    public Map<String, Object> receiveWorkbench(int page, int size) {
        Map<String, Object> result = new HashMap<>();
        Page<EmsSample> pending = sampleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<EmsSample>().eq(EmsSample::getStatus, "待收样").orderByDesc(EmsSample::getCreateTime));
        result.put("pendingSamples", pending.getRecords());
        result.put("pendingTotal", pending.getTotal());
        result.put("pendingStatusCount", sampleMapper.selectCount(new LambdaQueryWrapper<EmsSample>().eq(EmsSample::getStatus, "待收样")));
        return result;
    }

    private void syncOrderStatus(Long orderId, String status) {
        if (orderId == null) return;
        try {
            EmsSamplingOrder order = samplingOrderService.getById(orderId);
            if (order != null) {
                order.setStatus(status);
                samplingOrderService.updateById(order);
            }
        } catch (Exception ignored) {
        }
    }

    /** 样品关联的采样记录+订单信息，前端详情用 */
    public Map<String, Object> sampleDetail(Long sampleId) {
        EmsSample sample = sampleMapper.selectById(sampleId);
        if (sample == null) throw new BusinessException("样品不存在: " + sampleId);
        Map<String, Object> map = new HashMap<>();
        map.put("sample", sample);
        if (sample.getSamplingId() != null) {
            map.put("record", getById(sample.getSamplingId()));
        }
        if (sample.getOrderId() != null) {
            map.put("order", samplingOrderService.getById(sample.getOrderId()));
        }
        map.put("qcList", listQc(sampleId));
        map.put("logs", listLogs(sampleId));
        map.put("photos", listPhotos("sample", sampleId));
        return map;
    }
}
