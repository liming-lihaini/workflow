package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.ReceiveReq;
import com.flow.engine.dto.SampleCollectReq;
import com.flow.engine.dto.SampleDisposeReq;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.EmsPhoto;
import com.flow.engine.entity.EmsSample;
import com.flow.engine.entity.EmsSampleLog;
import com.flow.engine.entity.EmsSampleQcBinding;
import com.flow.engine.entity.EmsSamplingOrder;
import com.flow.engine.entity.EmsRetain;
import com.flow.engine.entity.EmsEntrust;
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
    @Autowired
    private EmsEntrustService entrustService;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired
    private com.flow.engine.engine.FlowEngine flowEngine;
    @Autowired
    private DictService dictService;

    /** 按字典 code + itemValue 查字典名称；未命中返回原值 */
    private String dictText(String dictCode, String itemValue) {
        if (!StringUtils.hasText(dictCode) || !StringUtils.hasText(itemValue)) return itemValue;
        List<DictItem> items = dictService.getDictItemsByCode(dictCode);
        for (DictItem it : items) {
            if (itemValue.equals(it.getItemValue())) return it.getItemText();
        }
        return itemValue;
    }

    // ===================== 采样记录 =====================

    public EmsSamplingRecord createRecord(EmsSamplingRecord rec) {
        if (rec.getOrderId() == null) throw new BusinessException("采样任务ID不能为空");
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

    /** 完成采样（采样中 → 采样完成），并同步采样任务状态 */
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

    /**
     * 清空全部采样派单数据：采样记录 + 采样任务 + 采样照片（bizType='sampling_record'）。
     * 仅清理采样业务线，不影响委托单/监测点位/样品/人员/设备等基础数据。物理删除，谨慎调用。
     * @return 清除的采样任务记录数
     */
    @Transactional
    public int clearAll() {
        // 先删子表，再删主表
        this.remove(null);                                   // t_sampling_record
        photoMapperRef.delete(new LambdaQueryWrapper<EmsPhoto>()
                .eq(EmsPhoto::getBizType, "sampling_record")); // t_photo（采样照片）
        int count = (int) samplingOrderService.count();
        samplingOrderService.remove(null);                   // t_sampling_order
        return count;
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

    /**
     * 手动收集样品：适用于未通过微信小程序上报/现场采样流程的场景。
     * 不依赖采样记录（samplingId 可空），创建后直接进入已收样状态，
     * 收样人/收样时间由调用方指定；同时写入收样日志。
     */
    @Transactional(rollbackFor = Exception.class)
    public EmsSample manualCollect(EmsSample sample) {
        if (!StringUtils.hasText(sample.getName())) throw new BusinessException("样品名称不能为空");
        // 默认来源标记，便于区分小程序上报的样品
        if (!StringUtils.hasText(sample.getSource())) sample.setSource("手动收集");
        // 生成条码
        int seq = (int) (sampleMapper.selectCount(new LambdaQueryWrapper<>()) + 1);
        sample.setBarcode(CodeGenerator.generate("YP", seq));
        sample.setStatus("已收样");
        sample.setReceiveBy(StringUtils.hasText(sample.getReceiveBy()) ? sample.getReceiveBy() : "收样员");
        sample.setReceiveTime(StringUtils.hasText(sample.getReceiveTime())
                ? sample.getReceiveTime() : LocalDate.now().toString());
        sample.setCreateTime(LocalDateTime.now());
        sample.setUpdateTime(LocalDateTime.now());
        sampleMapper.insert(sample);
        writeLog(sample.getId(), "手动收样", sample.getReceiveBy(),
                "手动收集样品: " + sample.getName() + "（条码 " + sample.getBarcode() + "）");
        return sample;
    }

    /**
     * 收样工作台-手动收集样品（采集版）：接收完整采集表单数据。
     * 关联采样派单/委托单/点位，保存检测类别/项目、采样参数值、固定剂、现场质控、
     * 留样标记与现场照片，生成样品条码并进入已收样状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public EmsSample manualCollect(SampleCollectReq req) {
        if (req.getDispatchId() == null) throw new BusinessException("请选择采样派单");
        if (req.getPointId() == null) throw new BusinessException("请选择监测点位");
        if (!StringUtils.hasText(req.getItem())) throw new BusinessException("请选择检测项目");
        if (!StringUtils.hasText(req.getName())) req.setName(
                (StringUtils.hasText(req.getCustName()) ? req.getCustName() : "样品")
                        + "-" + req.getItem());

        EmsSample sample = new EmsSample();
        sample.setDispatchId(req.getDispatchId());
        sample.setOrderId(req.getDispatchId());
        sample.setEntrustId(req.getEntrustId());
        sample.setPointId(req.getPointId());
        sample.setName(req.getName());
        sample.setType(req.getType());
        sample.setSource(req.getSource());
        sample.setAmount(req.getAmount());
        sample.setContainer(req.getContainer());
        sample.setPreserve(req.getPreserve());
        sample.setWeather(req.getWeather());
        sample.setSampler(req.getSampler());
        sample.setSampleTime(req.getSampleTime());
        sample.setRemark(req.getRemark());
        sample.setSampleNo(req.getSampleNo());
        sample.setCategory(req.getCategory());
        sample.setItem(req.getItem());
        // 采样参数值：列表序列化为 JSON 存储
        if (req.getSampleParams() != null && !req.getSampleParams().isEmpty()) {
            sample.setSampleParams(JsonUtils.toJson(req.getSampleParams()));
        }
        // 固定剂：多选合并
        sample.setPreservatives(req.getPreservatives() == null ? null
                : String.join(",", req.getPreservatives()));
        // 现场质控方式：多选合并
        sample.setQcTypes(req.getQcTypes() == null ? null : String.join(",", req.getQcTypes()));
        Integer retainSample = req.getRetainSample() == null ? 0 : req.getRetainSample();
        sample.setRetainSample(retainSample);
        // 现场照片：逗号分隔
        sample.setSamplePhoto(req.getPhotos() == null ? null : String.join(",", req.getPhotos()));

        sample.setSource("手动收集");
        // 收集保存后默认进入「待收样」状态，待收样阶段暂无收样人/收样时间，由收样环节填写
        sample.setStatus("待收样");
        int seq = (int) (sampleMapper.selectCount(new LambdaQueryWrapper<>()) + 1);
        sample.setBarcode(CodeGenerator.generate("YP", seq));
        sample.setCreateTime(LocalDateTime.now());
        sample.setUpdateTime(LocalDateTime.now());
        sampleMapper.insert(sample);
        // 留样信息：记录留样字段并登记留样库
        if (retainSample == 1) {
            Integer retainDays = req.getRetainDays() == null ? 0 : req.getRetainDays();
            String retainBy = StringUtils.hasText(req.getRetainBy()) ? req.getRetainBy() : sample.getReceiveBy();
            String retainDate = StringUtils.hasText(req.getRetainDate()) ? req.getRetainDate() : LocalDate.now().toString();
            if (StringUtils.hasText(req.getRetainLocation())) sample.setRetainLocation(req.getRetainLocation());
            sample.setRetainFlag(1);
            sample.setRetainBy(retainBy);
            sample.setRetainDate(retainDate);
            sample.setRetainDays(retainDays);
            LocalDate until = LocalDate.parse(retainDate).plusDays(retainDays);
            sample.setRetainUntil(until.toString());
            sampleMapper.updateById(sample);
            retain(sample.getId(), retainDays, null, retainBy, retainDate, req.getRetainLocation(), "手动收集留样");
            writeLog(sample.getId(), "留样", retainBy, "手动收集留样至 " + until + "（" + retainDays + "天）");
        }
        // 普通登记（不留样）保持上方设置的「待收样」状态；留样登记由上方更新为「留样中」
        // 注意：不再在此覆盖为「已收样」，收样由独立收样接口完成
        writeLog(sample.getId(), "手动收样", sample.getReceiveBy(),
                "手动收集样品: " + sample.getName() + "（条码 " + sample.getBarcode() + "，派单 "
                        + req.getDispatchNo() + "，检测项目 " + req.getItem() + "）");
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

    /**
     * 异常处置：仅更新异常处置相关字段（处置类型、处置方式、处置说明、处置人、处置时间）。
     * 只针对状态为「异常拒收」或「检测异常」的样品。
     */
    public EmsSample dispose(Long id, SampleDisposeReq req, String operator) {
        EmsSample exist = sampleMapper.selectById(id);
        if (exist == null) throw new BusinessException("样品不存在: " + id);
        if (!"异常拒收".equals(exist.getStatus()) && !"检测异常".equals(exist.getStatus())) {
            throw new BusinessException("仅「异常拒收」或「检测异常」的样品可提交异常处置");
        }
        LambdaUpdateWrapper<EmsSample> uw = new LambdaUpdateWrapper<>();
        String disposalTypeText = dictText("moni_disposal_type", req.getDisposalType());
        String disposalMethodText = dictText("moni_disposal_method", req.getDisposalMethod());
        uw.eq(EmsSample::getId, id)
                .set(EmsSample::getStatus, "已处置")
                .set(EmsSample::getDisposalType, disposalTypeText)
                .set(EmsSample::getDisposalMethod, disposalMethodText)
                .set(EmsSample::getDisposalDesc, req.getDisposalDesc())
                .set(EmsSample::getDisposalBy, operator)
                .set(EmsSample::getDisposalTime, LocalDateTime.now())
                .set(EmsSample::getUpdateTime, LocalDateTime.now());
        sampleMapper.update(null, uw);
        writeLog(id, "异常处置", operator,
                "处置类型: " + disposalTypeText + "，处置方式: " + disposalMethodText);
        return sampleMapper.selectById(id);
    }

    /** 收样：样品 待收样 → 已收样（正常）或 异常拒收。
     *  req.action = "reject" 时为异常拒收：状态置「异常拒收」，记录异常拒收日志，不办理留样。 */
    @Transactional(rollbackFor = Exception.class)
    public EmsSample receive(Long id, ReceiveReq req) {
        EmsSample exist = sampleMapper.selectById(id);
        if (exist == null) throw new BusinessException("样品不存在: " + id);
        // 允许对任意状态样品登记收样（列表展示全部，已收样/留样中可重新登记覆盖）
        exist.setReceiveBy(req.getReceiveBy());
        exist.setReceiveTime(req.getReceiveTime() == null ? LocalDate.now().toString() : req.getReceiveTime());
        if (StringUtils.hasText(req.getRemark())) exist.setRemark(req.getRemark());
        if (StringUtils.hasText(req.getAmount())) exist.setAmount(req.getAmount());
        if (StringUtils.hasText(req.getContainer())) exist.setContainer(req.getContainer());
        if (StringUtils.hasText(req.getPreserve())) exist.setPreserve(req.getPreserve());
        if (StringUtils.hasText(req.getCheckItems())) exist.setCheckItems(req.getCheckItems());

        boolean reject = "reject".equals(req.getAction());
        if (reject) {
            // 异常拒收：记录异常拒收日志，状态置「异常拒收」，不办理留样、不推进委托单
            exist.setStatus("异常拒收");
            exist.setUpdateTime(LocalDateTime.now());
            sampleMapper.updateById(exist);
            writeLog(id, "异常拒收", req.getReceiveBy(),
                    "收样异常拒收（收样人 " + req.getReceiveBy() + "，收样时间 "
                            + exist.getReceiveTime() + "）" + (StringUtils.hasText(req.getRemark())
                            ? "，原因：" + req.getRemark() : ""));
            return exist;
        }

        // 正常收样
        exist.setStatus("已收样");
        // 留样信息
        Integer retainFlag = req.getRetainFlag() == null ? 0 : req.getRetainFlag();
        exist.setRetainFlag(retainFlag);
        if (retainFlag == 1) {
            // 打开留样开关后，留样属性均为必填
            Integer retainDays = req.getRetainDays();
            if (retainDays == null || retainDays <= 0) throw new BusinessException("请填写留样保存天数");
            if (!StringUtils.hasText(req.getRetainAmount())) throw new BusinessException("请填写留样数量");
            if (!StringUtils.hasText(req.getRetainBy())) throw new BusinessException("请选择留样人");
            if (!StringUtils.hasText(req.getRetainLocation())) throw new BusinessException("请填写存放位置");
            String retainBy = req.getRetainBy();
            String retainDate = req.getRetainDate() == null ? LocalDate.now().toString() : req.getRetainDate();
            exist.setRetainLocation(req.getRetainLocation());
            exist.setRetainBy(retainBy);
            exist.setRetainAmount(req.getRetainAmount());
            exist.setRetainDate(retainDate);
            exist.setRetainDays(retainDays);
            LocalDate until = LocalDate.parse(retainDate).plusDays(retainDays);
            exist.setRetainUntil(until.toString());
            exist.setUpdateTime(LocalDateTime.now());
            sampleMapper.updateById(exist);
            // 写入留样库
            retain(id, retainDays, req.getRetainAmount(), retainBy, retainDate, req.getRetainLocation(), "收样登记留样");
            writeLog(id, "收样", req.getReceiveBy(), "收样时间: " + exist.getReceiveTime() + "，已登记留样");
        } else {
            exist.setUpdateTime(LocalDateTime.now());
            sampleMapper.updateById(exist);
            writeLog(id, "收样", req.getReceiveBy(), "收样时间: " + exist.getReceiveTime());
        }
        // 收样完成后不再自动推进委托单状态（样品提交不驱动检测委托单状态修改）
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
    public EmsRetain retain(Long sampleId, Integer retainDays, String retainAmount, String retainBy, String retainTime, String retainLocation, String remark) {
        EmsSample sample = sampleMapper.selectById(sampleId);
        if (sample == null) throw new BusinessException("样品不存在: " + sampleId);
        if (retainDays == null || retainDays <= 0) throw new BusinessException("留样天数必须大于0");
        LocalDate start = retainTime == null ? LocalDate.now() : LocalDate.parse(retainTime);
        LocalDate until = start.plusDays(retainDays);
        EmsRetain retain = new EmsRetain();
        retain.setSampleId(sampleId);
        retain.setBarcode(sample.getBarcode());
        retain.setName(sample.getName());
        retain.setCategory(sample.getCategory());
        retain.setRetainLocation(retainLocation);
        retain.setPointId(sample.getPointId());
        retain.setRetainBy(retainBy);
        retain.setRetainTime(start.toString());
        retain.setRetainDays(retainDays);
        if (StringUtils.hasText(retainAmount)) retain.setRetainAmount(retainAmount);
        retain.setRetainUntil(until.toString()); 
        retain.setStatus("留样中");
        retain.setRemark(remark);
        // 生成留样编号：LY + yyyyMMdd + 三位序号（当日已有留样数 + 1）
        String dateStr = start.toString().replace("-", "");
        long todayCount = retainService.count(new LambdaQueryWrapper<EmsRetain>()
                .likeRight(EmsRetain::getRetainNo, "LY" + dateStr));
        retain.setRetainNo(String.format("LY%s%03d", dateStr, todayCount + 1));
        retain.setCreateTime(LocalDateTime.now());
        retain.setUpdateTime(LocalDateTime.now());
        retainService.save(retain);

        sample.setRetainFlag(1);
        sample.setRetainDays(retainDays);
        if (StringUtils.hasText(retainAmount)) sample.setRetainAmount(retainAmount);
        sample.setRetainUntil(until.toString());
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

    /** 留样库统计：在库留样数 / 3日内到期数 / 待销毁审批数 / 本月复检领用数 */
    public Map<String, Object> retainStats() {
        Map<String, Object> m = new HashMap<>();
        // 在库留样（status=留样中）
        long inStock = retainService.count(new LambdaQueryWrapper<EmsRetain>()
                .eq(EmsRetain::getStatus, "留样中"));
        // 3日内到期（status=留样中 且 retain_until <= now+3）
        LocalDate line3 = LocalDate.now().plusDays(3);
        long expireSoon = retainService.count(new LambdaQueryWrapper<EmsRetain>()
                .eq(EmsRetain::getStatus, "留样中")
                .le(EmsRetain::getRetainUntil, line3));
        // 待销毁审批（status=销毁审批中）
        long pendingDispose = retainService.count(new LambdaQueryWrapper<EmsRetain>()
                .eq(EmsRetain::getStatus, "销毁审批中"));
        // 本月复检领用（暂无领用记录表，返回0，后续扩展）
        long monthlyReuse = 0;
        m.put("inStock", inStock);
        m.put("expireSoon", expireSoon);
        m.put("pendingDispose", pendingDispose);
        m.put("monthlyReuse", monthlyReuse);
        return m;
    }

    /** 留样销毁申请：更新留样状态 + 发起审批流程 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyDispose(Long retainId, String startUser, Map<String, Object> formData) {
        EmsRetain retain = retainService.getById(retainId);
        if (retain == null) throw new BusinessException("留样记录不存在: " + retainId);
        if (!"留样中".equals(retain.getStatus())) throw new BusinessException("仅留样中的样品可申请销毁");
        // 收集表单数据
        String disposeReason = formData != null ? (String) formData.get("disposeReason") : null;
        String disposeMethod = formData != null ? (String) formData.get("disposeMethod") : null;
        String disposeDate = formData != null ? (String) formData.get("disposeDate") : null;

        // 更新留样记录
        retain.setStatus("销毁审批中");
        retain.setDisposeReason(disposeReason);
        retain.setDisposeMethod(disposeMethod);
        retain.setDisposeDate(disposeDate);
        retain.setUpdateTime(LocalDateTime.now());
        retainService.updateById(retain);

        // 发起流程，留样信息 + 销毁信息作为流程变量传入（表单回填用）
        Map<String, Object> variables = new HashMap<>();
        variables.put("retainId", retainId);
        variables.put("retainNo", retain.getRetainNo());
        variables.put("barcode", retain.getBarcode());
        variables.put("category", retain.getCategory());
        variables.put("disposeReason", disposeReason);
        variables.put("disposeMethod", disposeMethod);
        variables.put("disposeDate", disposeDate);

        com.flow.engine.entity.ProcessInstance inst = flowEngine.startProcess(
                "LYXHSQ", String.valueOf(retainId), startUser, variables);

        // 关联流程实例ID
        retain.setProcessInstanceId(inst.getId());
        retainService.updateById(retain);

        Map<String, Object> result = new HashMap<>();
        result.put("retainId", retainId);
        result.put("processInstanceId", inst.getId());
        result.put("retainNo", retain.getRetainNo());
        return result;
    }

    // ===================== 聚合 =====================

    /** 收样工作台：返回待收样样品 + 已完成采样记录，便于一键收样 */
    public Map<String, Object> receiveWorkbench(int page, int size) {
        Map<String, Object> result = new HashMap<>();
        // 展示全部样品，不进行状态过滤
        Page<EmsSample> pending = sampleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<EmsSample>().orderByDesc(EmsSample::getCreateTime));
        result.put("pendingSamples", pending.getRecords());
        result.put("pendingTotal", pending.getTotal());
        result.put("pendingStatusCount", sampleMapper.selectCount(new LambdaQueryWrapper<>()));
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
        // 采样点位名：手动收集场景无采样记录，通过 pointId 查 t_monitor_point 补充
        if (sample.getPointId() != null) {
            try {
                String pointName = jdbcTemplate.queryForObject(
                        "SELECT point_name FROM t_monitor_point WHERE id = ?",
                        String.class, sample.getPointId());
                map.put("pointName", pointName);
            } catch (Exception e) { /* ignore */ }
        }
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
