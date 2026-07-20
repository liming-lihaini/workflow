package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.DictType;
import com.flow.engine.mapper.DictItemMapper;
import com.flow.engine.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据字典服务（ISSUE-015, TRD §5.5.6）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    /**
     * 获取字典类型列表
     */
    public List<DictType> getDictTypes(String dictName, String dictCode, Integer dictType, Integer status) {
        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(dictName)) {
            wrapper.like(DictType::getDictName, dictName);
        }
        if (StringUtils.hasText(dictCode)) {
            wrapper.like(DictType::getDictCode, dictCode);
        }
        if (dictType != null) {
            wrapper.eq(DictType::getDictType, dictType);
        }
        if (status != null) {
            wrapper.eq(DictType::getStatus, status);
        }
        
        wrapper.orderByAsc(DictType::getId);
        return dictTypeMapper.selectList(wrapper);
    }

    /**
     * 获取字典类型详情
     */
    public DictType getDictType(Long id) {
        DictType dictType = dictTypeMapper.selectById(id);
        if (dictType == null) {
            throw new BusinessException(ErrorCode.DICT_TYPE_NOT_FOUND);
        }
        return dictType;
    }

    /**
     * 创建字典类型
     */
    public DictType createDictType(DictType dictType) {
        // 校验编码唯一性
        DictType existing = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<DictType>().eq(DictType::getDictCode, dictType.getDictCode())
        );
        if (existing != null) {
            throw new BusinessException(ErrorCode.DICT_CODE_DUPLICATE);
        }
        
        if (dictType.getStatus() == null) {
            dictType.setStatus(1);
        }
        if (dictType.getDictType() == null) {
            dictType.setDictType(2); // 默认业务自定义
        }
        dictType.setCreateTime(LocalDateTime.now());
        dictType.setUpdateTime(LocalDateTime.now());
        
        dictTypeMapper.insert(dictType);
        log.info("[DictService] 创建字典类型: id={}, code={}", dictType.getId(), dictType.getDictCode());
        return dictType;
    }

    /**
     * 更新字典类型
     */
    public DictType updateDictType(Long id, DictType dictType) {
        DictType existing = getDictType(id);
        
        // 如果修改了编码，校验唯一性
        if (dictType.getDictCode() != null && !dictType.getDictCode().equals(existing.getDictCode())) {
            DictType duplicate = dictTypeMapper.selectOne(
                    new LambdaQueryWrapper<DictType>().eq(DictType::getDictCode, dictType.getDictCode())
            );
            if (duplicate != null) {
                throw new BusinessException(ErrorCode.DICT_CODE_DUPLICATE);
            }
        }
        
        if (dictType.getDictName() != null) {
            existing.setDictName(dictType.getDictName());
        }
        if (dictType.getDictCode() != null) {
            existing.setDictCode(dictType.getDictCode());
        }
        if (dictType.getDescription() != null) {
            existing.setDescription(dictType.getDescription());
        }
        if (dictType.getStatus() != null) {
            existing.setStatus(dictType.getStatus());
        }
        existing.setUpdateTime(LocalDateTime.now());
        
        dictTypeMapper.updateById(existing);
        log.info("[DictService] 更新字典类型: id={}", id);
        return existing;
    }

    /**
     * 删除字典类型（系统内置不可删除）
     */
    public void deleteDictType(Long id) {
        DictType dictType = getDictType(id);
        
        // 系统内置字典不可删除
        if (dictType.getDictType() != null && dictType.getDictType() == 1) {
            throw new BusinessException(ErrorCode.DICT_TYPE_BUILTIN);
        }
        
        // 删除关联的字典项
        dictItemMapper.delete(
                new LambdaQueryWrapper<DictItem>().eq(DictItem::getDictTypeId, id)
        );
        
        dictTypeMapper.deleteById(id);
        log.info("[DictService] 删除字典类型: id={}, code={}", id, dictType.getDictCode());
    }

    /**
     * 获取字典项列表
     */
    public List<DictItem> getDictItems(Long dictTypeId, Integer status) {
        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<>();
        
        if (dictTypeId != null) {
            wrapper.eq(DictItem::getDictTypeId, dictTypeId);
        }
        if (status != null) {
            wrapper.eq(DictItem::getStatus, status);
        }
        
        wrapper.orderByAsc(DictItem::getSortOrder);
        return dictItemMapper.selectList(wrapper);
    }

    /**
     * 根据字典类型ID获取字典项
     */
    public List<DictItem> getDictItemsByTypeId(Long typeId) {
        return getDictItems(typeId, 1); // 只返回正常状态的项
    }

    /**
     * 根据字典编码获取字典项
     */
    public List<DictItem> getDictItemsByCode(String dictCode) {
        // 先根据编码找到字典类型
        DictType dictType = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<DictType>().eq(DictType::getDictCode, dictCode)
        );
        if (dictType == null) {
            throw new BusinessException(ErrorCode.DICT_TYPE_NOT_FOUND);
        }
        
        return getDictItemsByTypeId(dictType.getId());
    }

    /**
     * 创建字典项
     */
    public DictItem createDictItem(DictItem dictItem) {
        // 校验字典类型存在
        getDictType(dictItem.getDictTypeId());
        
        if (dictItem.getStatus() == null) {
            dictItem.setStatus(1);
        }
        if (dictItem.getSortOrder() == null) {
            dictItem.setSortOrder(0);
        }
        dictItem.setCreateTime(LocalDateTime.now());
        dictItem.setUpdateTime(LocalDateTime.now());
        
        dictItemMapper.insert(dictItem);
        log.info("[DictService] 创建字典项: id={}, typeId={}, text={}", 
                dictItem.getId(), dictItem.getDictTypeId(), dictItem.getItemText());
        return dictItem;
    }

    /**
     * 更新字典项
     */
    public DictItem updateDictItem(Long id, DictItem dictItem) {
        DictItem existing = dictItemMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DICT_ITEM_NOT_FOUND);
        }
        
        if (dictItem.getItemText() != null) {
            existing.setItemText(dictItem.getItemText());
        }
        if (dictItem.getItemValue() != null) {
            existing.setItemValue(dictItem.getItemValue());
        }
        if (dictItem.getSortOrder() != null) {
            existing.setSortOrder(dictItem.getSortOrder());
        }
        if (dictItem.getStatus() != null) {
            existing.setStatus(dictItem.getStatus());
        }
        existing.setUpdateTime(LocalDateTime.now());
        
        dictItemMapper.updateById(existing);
        log.info("[DictService] 更新字典项: id={}", id);
        return existing;
    }

    /**
     * 删除字典项
     */
    public void deleteDictItem(Long id) {
        DictItem dictItem = dictItemMapper.selectById(id);
        if (dictItem == null) {
            throw new BusinessException(ErrorCode.DICT_ITEM_NOT_FOUND);
        }
        
        dictItemMapper.deleteById(id);
        log.info("[DictService] 删除字典项: id={}", id);
    }
}
