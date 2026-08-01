package com.flow.engine.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsDepartment;
import com.flow.engine.mapper.EmsDepartmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 环境监测 - 部门服务（TRD 5.10）
 * 业务规则：BR-022-07 部门名必填；BR-022-08 父部门必须存在或为 0（根）
 */
@Service
public class EmsDepartmentService extends ServiceImpl<EmsDepartmentMapper, EmsDepartment> {

    public EmsDepartment create(EmsDepartment d) {
        if (!StringUtils.hasText(d.getDeptName())) {
            throw new IllegalArgumentException("部门名称不能为空(BR-022-07)");
        }
        if (d.getParentId() != null && d.getParentId() != 0 && this.getById(d.getParentId()) == null) {
            throw new IllegalArgumentException("父部门不存在(BR-022-08)");
        }
        d.setStatus(d.getStatus() == null ? 1 : d.getStatus());
        d.setCreateTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        this.save(d);
        return d;
    }
}
