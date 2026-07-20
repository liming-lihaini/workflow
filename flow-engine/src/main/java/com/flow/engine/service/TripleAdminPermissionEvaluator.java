package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.enums.AdminType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 三员权限决策器（ISSUE-016, TRD §5.5.8）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripleAdminPermissionEvaluator {

    private final TripleAdminService tripleAdminService;

    /**
     * 系统管理员权限校验
     * - 可管理普通用户
     * - 可进行系统配置
     * - 不可管理安全员、审计员
     */
    public boolean canManageUser(Long operatorId, Long targetId) {
        // 如果操作者不是系统管理员，交给其他权限系统判断
        if (!tripleAdminService.isSystemAdmin(operatorId)) {
            return true;
        }
        
        // 系统管理员不能管理其他三员
        if (tripleAdminService.isTripleAdmin(targetId)) {
            return false;
        }
        
        return true;
    }

    /**
     * 安全管理员权限校验
     * - 可分配角色权限
     * - 可管理安全策略
     * - 不可管理系统管理员、审计员
     */
    public boolean canManageSecurity(Long operatorId, Long targetId) {
        // 如果操作者不是安全管理员，交给其他权限系统判断
        if (!tripleAdminService.isSecurityAdmin(operatorId)) {
            return true;
        }
        
        // 安全管理员不能管理其他三员
        if (tripleAdminService.isTripleAdmin(targetId)) {
            return false;
        }
        
        return true;
    }

    /**
     * 审计管理员权限校验
     * - 可查看所有日志
     * - 不可进行业务操作
     * - 不可管理系统管理员、安全员
     */
    public boolean canManageAudit(Long operatorId, Long targetId) {
        // 如果操作者不是审计管理员，交给其他权限系统判断
        if (!tripleAdminService.isAuditAdmin(operatorId)) {
            return true;
        }
        
        // 审计管理员不能管理任何三员
        if (tripleAdminService.isTripleAdmin(targetId)) {
            return false;
        }
        
        // 审计管理员不能进行业务写操作
        return false;
    }

    /**
     * 校验三员不能删除自己的账号
     */
    public boolean canDeleteSelf(Long operatorId, Long targetId) {
        // 三员不能删除自己
        if (tripleAdminService.isTripleAdmin(operatorId) && operatorId.equals(targetId)) {
            return false;
        }
        return true;
    }

    /**
     * 综合权限校验：检查操作者是否可以对目标用户执行操作
     */
    public void checkPermission(Long operatorId, Long targetId, String operation) {
        // 校验不能删除自己
        if ("delete".equals(operation)) {
            if (!canDeleteSelf(operatorId, targetId)) {
                throw new BusinessException(ErrorCode.ADMIN_CANNOT_DELETE_SELF);
            }
        }
        
        // 获取操作者的三员类型
        AdminType operatorType = tripleAdminService.getAdminType(operatorId);
        
        if (operatorType == null) {
            // 非三员，不限制
            return;
        }
        
        // 获取目标的三员类型
        AdminType targetType = tripleAdminService.getAdminType(targetId);
        
        // 三员之间不能互相操作
        if (targetType != null) {
            throw new BusinessException(ErrorCode.ADMIN_CANNOT_OPERATE_OTHER);
        }
        
        // 审计管理员不能进行业务写操作
        if (operatorType == AdminType.AUDIT_ADMIN && !"query".equals(operation) && !"view".equals(operation)) {
            throw new BusinessException(ErrorCode.ADMIN_OPERATION_DENIED, "审计管理员只能查看日志，不能进行业务操作");
        }
    }
}
