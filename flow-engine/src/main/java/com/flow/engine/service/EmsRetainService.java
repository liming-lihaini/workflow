package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsRetain;
import com.flow.engine.mapper.EmsRetainMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 环境监测 - 留样库管理（TRD 5.3 / ISSUE-024）
 */
@Service
public class EmsRetainService extends ServiceImpl<EmsRetainMapper, EmsRetain> {

    public Page<EmsRetain> pageRetain(String keyword, String status, int page, int size) {
        LambdaQueryWrapper<EmsRetain> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) q.eq(EmsRetain::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            q.like(EmsRetain::getName, keyword).or().like(EmsRetain::getBarcode, keyword);
        }
        q.orderByDesc(EmsRetain::getCreateTime);
        return this.page(new Page<>(page, size), q);
    }
}
