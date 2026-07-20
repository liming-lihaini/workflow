package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.entity.Dept;
import com.flow.engine.entity.User;
import com.flow.engine.mapper.DeptMapper;
import com.flow.engine.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门管理服务（ISSUE-013）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    /**
     * 创建部门
     */
    public Dept createDept(Dept dept) {
        dept.setStatus(1);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.insert(dept);
        log.info("创建部门: id={}, name={}", dept.getId(), dept.getDeptName());
        return dept;
    }

    /**
     * 获取部门详情
     */
    public Dept getDept(Long id) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        }
        return dept;
    }

    /**
     * 获取部门列表
     */
    public List<Dept> listDepts() {
        return deptMapper.selectList(new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getSortOrder));
    }

    /**
     * 分页查询部门（支持条件搜索）
     */
    public Map<String, Object> listDeptsPage(String deptName, Integer status, int page, int size) {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        if (deptName != null && !deptName.isEmpty()) {
            wrapper.like(Dept::getDeptName, deptName);
        }
        if (status != null) {
            wrapper.eq(Dept::getStatus, status);
        }
        wrapper.orderByAsc(Dept::getSortOrder);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Dept> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Dept> result =
                deptMapper.selectPage(pageParam, wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    /**
     * 获取部门树
     */
    public List<Map<String, Object>> getDeptTree() {
        List<Dept> allDepts = listDepts();
        return buildTree(allDepts, 0L);
    }

    /**
     * 更新部门
     */
    public Dept updateDept(Long id, Dept dept) {
        Dept existing = getDept(id);
        if (dept.getDeptName() != null) existing.setDeptName(dept.getDeptName());
        if (dept.getDeptCode() != null) existing.setDeptCode(dept.getDeptCode());
        if (dept.getDeptType() != null) existing.setDeptType(dept.getDeptType());
        if (dept.getSortOrder() != null) existing.setSortOrder(dept.getSortOrder());
        if (dept.getParentId() != null) existing.setParentId(dept.getParentId());
        if (dept.getPhone() != null) existing.setPhone(dept.getPhone());
        if (dept.getStatus() != null) existing.setStatus(dept.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        deptMapper.updateById(existing);
        log.info("更新部门: id={}", id);
        return existing;
    }

    /**
     * 删除部门
     */
    public void deleteDept(Long id) {
        getDept(id);
        // 检查子部门
        Long childCount = deptMapper.selectCount(new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN);
        }
        // 检查用户
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_USERS);
        }
        deptMapper.deleteById(id);
        log.info("删除部门: id={}", id);
    }

    /**
     * 设置部门领导
     */
    public Dept setLeader(Long deptId, Long leaderId, String leaderName) {
        Dept dept = getDept(deptId);
        dept.setLeaderId(leaderId);
        dept.setLeaderName(leaderName);
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.updateById(dept);
        log.info("设置部门领导: deptId={}, leaderId={}", deptId, leaderId);
        return dept;
    }

    /**
     * 获取子部门ID列表（递归）
     */
    public List<Long> getChildDeptIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        collectChildIds(parentId, result);
        return result;
    }

    private void collectChildIds(Long parentId, List<Long> result) {
        List<Dept> children = deptMapper.selectList(
                new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, parentId));
        for (Dept child : children) {
            result.add(child.getId());
            collectChildIds(child.getId(), result);
        }
    }

    private List<Map<String, Object>> buildTree(List<Dept> depts, Long parentId) {
        return depts.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .map(d -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", d.getId());
                    node.put("deptName", d.getDeptName());
                    node.put("deptCode", d.getDeptCode());
                    node.put("leaderName", d.getLeaderName());
                    node.put("status", d.getStatus());
                    node.put("children", buildTree(depts, d.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }
}
