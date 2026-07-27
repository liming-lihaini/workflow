package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.dto.CreateDelegationRequest;
import com.flow.engine.dto.DelegationResponse;
import com.flow.engine.entity.Delegation;
import com.flow.engine.entity.User;
import com.flow.engine.mapper.DelegationMapper;
import com.flow.engine.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局委托服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelegationService {

    private final DelegationMapper delegationMapper;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;

    private static final String TODO_CACHE_PREFIX = "task:todo:";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建全局委托
     */
    @Transactional
    public DelegationResponse createDelegation(CreateDelegationRequest request) {
        String delegatorId = request.getOperatorId();
        String delegateId = request.getDelegateUserId();

        if (!StringUtils.hasText(delegatorId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "委托人不能为空");
        }
        if (!StringUtils.hasText(delegateId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "代理人不能为空");
        }
        if (delegatorId.equals(delegateId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "代理人不能是自己");
        }

        // 检查循环依赖
        if (hasCircularDelegation(delegatorId, delegateId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "不能创建循环委托");
        }

        // 解析时间
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        if (startTime == null) startTime = LocalDateTime.now();
        if (endTime != null && endTime.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "结束时间不能早于开始时间");
        }

        // 检查是否已有生效中的委托（同一代理人）
        LambdaQueryWrapper<Delegation> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(Delegation::getDelegatorId, delegatorId)
                .eq(Delegation::getDelegateId, delegateId)
                .eq(Delegation::getStatus, 0);
        Delegation existing = delegationMapper.selectOne(existQuery);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "已有对该代理人生效中的委托，请先取消或修改");
        }

        // 创建委托记录
        Delegation delegation = new Delegation();
        delegation.setDelegatorId(delegatorId);
        delegation.setDelegateId(delegateId);
        delegation.setStartTime(startTime);
        delegation.setEndTime(endTime);
        delegation.setReason(request.getReason());
        delegation.setStatus(0); // 生效中
        delegation.setCreateTime(LocalDateTime.now());
        delegation.setUpdateTime(LocalDateTime.now());
        delegationMapper.insert(delegation);

        // 清除代理人待办缓存（代理人现在可以看到委托人的任务）
        evictTodoCache(delegateId);

        log.info("[DelegationService] 全局委托创建: 委托人={}, 代理人={}, id={}", delegatorId, delegateId, delegation.getId());
        return toResponse(delegation);
    }

    /**
     * 取消委托（仅委托人可操作）
     */
    @Transactional
    public void cancelDelegation(Long delegationId, String operatorId) {
        Delegation delegation = delegationMapper.selectById(delegationId);
        if (delegation == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "委托记录不存在");
        }
        if (delegation.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "该委托已不是生效状态");
        }
        if (!delegation.getDelegatorId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "只有委托人可以取消委托");
        }

        delegation.setStatus(2); // 已取消
        delegation.setUpdateTime(LocalDateTime.now());
        delegationMapper.updateById(delegation);

        evictTodoCache(delegation.getDelegatorId());
        evictTodoCache(delegation.getDelegateId());

        log.info("[DelegationService] 委托取消: id={}, 委托人={}", delegationId, operatorId);
    }

    /**
     * 检查过期委托并自动取消
     */
    @Transactional
    public int expireDelegations() {
        LambdaQueryWrapper<Delegation> query = new LambdaQueryWrapper<>();
        query.eq(Delegation::getStatus, 0)
                .isNotNull(Delegation::getEndTime)
                .lt(Delegation::getEndTime, LocalDateTime.now());
        List<Delegation> expired = delegationMapper.selectList(query);

        for (Delegation d : expired) {
            d.setStatus(3); // 已过期
            d.setUpdateTime(LocalDateTime.now());
            delegationMapper.updateById(d);

            evictTodoCache(d.getDelegatorId());
            evictTodoCache(d.getDelegateId());

            log.info("[DelegationService] 委托过期: id={}, 委托人={}, 代理人={}", d.getId(), d.getDelegatorId(), d.getDelegateId());
        }

        return expired.size();
    }

    /**
     * 查询"我的委托"记录（当前用户作为委托人）
     */
    public List<DelegationResponse> getDelegationsByDelegator(String userId) {
        LambdaQueryWrapper<Delegation> query = new LambdaQueryWrapper<>();
        query.eq(Delegation::getDelegatorId, userId)
                .orderByDesc(Delegation::getCreateTime);
        return delegationMapper.selectList(query).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询"代理我的"记录（当前用户作为代理人）
     */
    public List<DelegationResponse> getDelegationsByDelegate(String userId) {
        LambdaQueryWrapper<Delegation> query = new LambdaQueryWrapper<>();
        query.eq(Delegation::getDelegateId, userId)
                .orderByDesc(Delegation::getCreateTime);
        return delegationMapper.selectList(query).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户所有生效委托中的委托人ID列表（供待办查询使用）
     * @param userId 当前用户（代理人）
     * @return 委托人ID列表
     */
    public List<String> getActiveDelegatorIds(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Delegation> query = new LambdaQueryWrapper<>();
        query.eq(Delegation::getDelegateId, userId)
                .eq(Delegation::getStatus, 0)
                .le(Delegation::getStartTime, now)
                .and(w -> w.isNull(Delegation::getEndTime).or().ge(Delegation::getEndTime, now));
        List<Delegation> active = delegationMapper.selectList(query);
        return active.stream().map(Delegation::getDelegatorId).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 检查循环依赖：从 delegateId 出发，沿委托链追溯，如果能回到 delegatorId 则拒绝
     */
    private boolean hasCircularDelegation(String delegatorId, String delegateId) {
        Set<String> visited = new HashSet<>();
        String current = delegateId;
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            if (current.equals(delegatorId)) return true;
            // 查询 current 作为代理人时，其委托人是谁
            LambdaQueryWrapper<Delegation> q = new LambdaQueryWrapper<>();
            q.eq(Delegation::getDelegateId, current).eq(Delegation::getStatus, 0);
            Delegation d = delegationMapper.selectOne(q);
            current = (d != null) ? d.getDelegatorId() : null;
        }
        return false;
    }

    private DelegationResponse toResponse(Delegation d) {
        DelegationResponse resp = new DelegationResponse();
        resp.setId(d.getId());
        resp.setDelegatorId(d.getDelegatorId());
        resp.setDelegateId(d.getDelegateId());
        resp.setStartTime(d.getStartTime());
        resp.setEndTime(d.getEndTime());
        resp.setReason(d.getReason());
        resp.setStatus(d.getStatus());
        resp.setCreateTime(d.getCreateTime());
        resp.setUpdateTime(d.getUpdateTime());

        // 状态描述
        switch (d.getStatus()) {
            case 0: resp.setStatusDesc("生效中"); break;
            case 2: resp.setStatusDesc("已取消"); break;
            case 3: resp.setStatusDesc("已过期"); break;
            default: resp.setStatusDesc("未知");
        }

        // 查询用户姓名
        try {
            User delegator = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, d.getDelegatorId()).last("LIMIT 1"));
            if (delegator != null) resp.setDelegatorName(delegator.getRealName());
        } catch (Exception ignored) {}
        try {
            User delegate = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, d.getDelegateId()).last("LIMIT 1"));
            if (delegate != null) resp.setDelegateName(delegate.getRealName());
        } catch (Exception ignored) {}

        return resp;
    }

    private LocalDateTime parseDateTime(String timeStr) {
        if (!StringUtils.hasText(timeStr)) return null;
        try {
            return LocalDateTime.parse(timeStr, DT_FMT);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(timeStr + " 00:00:00", DT_FMT);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private void evictTodoCache(String userId) {
        if (StringUtils.hasText(userId)) {
            Cache cache = cacheManager.getCache(TODO_CACHE_PREFIX + userId);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
