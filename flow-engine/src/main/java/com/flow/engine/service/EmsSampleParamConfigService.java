package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsSampleParamConfig;
import com.flow.engine.entity.EmsSampleParamItem;
import com.flow.engine.mapper.EmsSampleParamConfigMapper;
import com.flow.engine.mapper.EmsSampleParamItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 环境监测 - 采样参数配置服务（TRD 5.1 采样参数配置管理）
 * 主表 t_sample_param_config + 明细表 t_sample_param_item（一对多）。
 */
@Service
public class EmsSampleParamConfigService extends ServiceImpl<EmsSampleParamConfigMapper, EmsSampleParamConfig> {

    private final EmsSampleParamItemMapper itemMapper;

    public EmsSampleParamConfigService(EmsSampleParamItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    /** 条件检索：按检测类别 + 检测项目关键字过滤 */
    public List<Map<String, Object>> search(String type, String keyword) {
        LambdaQueryWrapper<EmsSampleParamConfig> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.hasText(type), EmsSampleParamConfig::getType, type)
          .like(StringUtils.hasText(keyword), EmsSampleParamConfig::getItem, keyword)
          .orderByDesc(EmsSampleParamConfig::getId);
        List<EmsSampleParamConfig> configs = this.list(qw);
        List<Long> ids = configs.stream().map(EmsSampleParamConfig::getId).collect(Collectors.toList());
        Map<Long, List<EmsSampleParamItem>> itemsMap = itemsByConfigIds(ids);
        List<Map<String, Object>> result = new ArrayList<>();
        for (EmsSampleParamConfig c : configs) {
            result.add(toVo(c, itemsMap.get(c.getId())));
        }
        return result;
    }

    /** 详情（含结构化参数明细） */
    public Map<String, Object> detail(Long id) {
        EmsSampleParamConfig c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("采样参数配置不存在: " + id);
        }
        return toVo(c, itemsByConfigIds(List.of(id)).get(id));
    }

    /** 新建 / 更新（级联保存明细）；id 为空为新建，否则为更新 */
    @Transactional
    public Map<String, Object> saveConfig(EmsSampleParamConfig config, List<EmsSampleParamItem> items) {
        if (!StringUtils.hasText(config.getType())) {
            throw new IllegalArgumentException("检测类别不能为空");
        }
        if (!StringUtils.hasText(config.getItem())) {
            throw new IllegalArgumentException("检测项目不能为空");
        }
        boolean isNew = config.getId() == null;
        LocalDateTime now = LocalDateTime.now();
        if (isNew) {
            config.setCreateTime(now);
        }
        config.setUpdateTime(now);
        this.saveOrUpdate(config);
        Long configId = config.getId();

        // 删除旧明细并重新写入（简单可靠）
        itemMapper.delete(new LambdaQueryWrapper<EmsSampleParamItem>().eq(EmsSampleParamItem::getConfigId, configId));
        if (items != null) {
            int idx = 0;
            for (EmsSampleParamItem it : items) {
                it.setId(null);
                it.setConfigId(configId);
                it.setSortNo(idx++);
                itemMapper.insert(it);
            }
        }
        return detail(configId);
    }

    /** 删除配置（级联删除明细） */
    @Transactional
    public void removeConfig(Long id) {
        itemMapper.delete(new LambdaQueryWrapper<EmsSampleParamItem>().eq(EmsSampleParamItem::getConfigId, id));
        this.removeById(id);
    }

    /** 批量删除 */
    @Transactional
    public void removeBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        itemMapper.delete(new LambdaQueryWrapper<EmsSampleParamItem>().in(EmsSampleParamItem::getConfigId, ids));
        this.removeByIds(ids);
    }

    // ===== 私有辅助 =====

    private Map<Long, List<EmsSampleParamItem>> itemsByConfigIds(List<Long> ids) {
        Map<Long, List<EmsSampleParamItem>> map = new java.util.HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        List<EmsSampleParamItem> all = itemMapper.selectList(
                new LambdaQueryWrapper<EmsSampleParamItem>()
                        .in(EmsSampleParamItem::getConfigId, ids)
                        .orderByAsc(EmsSampleParamItem::getSortNo));
        for (EmsSampleParamItem it : all) {
            map.computeIfAbsent(it.getConfigId(), k -> new ArrayList<>()).add(it);
        }
        return map;
    }

    private Map<String, Object> toVo(EmsSampleParamConfig c, List<EmsSampleParamItem> items) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", c.getId());
        vo.put("type", c.getType());
        vo.put("item", c.getItem());
        vo.put("standard", c.getStandard());
        vo.put("limit", c.getLimitValue());
        vo.put("unit", c.getUnit());
        vo.put("innerLimit", c.getInnerLimit());
        vo.put("remark", c.getRemark());
        vo.put("createTime", c.getCreateTime());
        vo.put("updateTime", c.getUpdateTime());
        vo.put("sampleParams", items == null ? new ArrayList<>() : items);
        return vo;
    }
}
