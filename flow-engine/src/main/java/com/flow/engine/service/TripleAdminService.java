package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.enums.AdminType;
import com.flow.engine.entity.OperationLog;
import com.flow.engine.entity.Role;
import com.flow.engine.entity.User;
import com.flow.engine.entity.UserRole;
import com.flow.engine.mapper.OperationLogMapper;
import com.flow.engine.mapper.RoleMapper;
import com.flow.engine.mapper.UserMapper;
import com.flow.engine.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 三员管理服务（ISSUE-016, TRD §5.5.7）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripleAdminService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final OperationLogMapper operationLogMapper;
    private final AuthService authService;

    /**
     * 获取管理员类型列表
     */
    public List<Map<String, Object>> getAdminTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (AdminType type : AdminType.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("code", type.getCode());
            map.put("roleKey", type.getRoleKey());
            map.put("name", type.getName());
            types.add(map);
        }
        return types;
    }

    /**
     * 判断用户是否为系统管理员
     */
    public boolean isSystemAdmin(Long userId) {
        return hasAdminRole(userId, AdminType.SYSTEM_ADMIN);
    }

    /**
     * 判断用户是否为安全管理员
     */
    public boolean isSecurityAdmin(Long userId) {
        return hasAdminRole(userId, AdminType.SECURITY_ADMIN);
    }

    /**
     * 判断用户是否为审计管理员
     */
    public boolean isAuditAdmin(Long userId) {
        return hasAdminRole(userId, AdminType.AUDIT_ADMIN);
    }

    /**
     * 判断用户是否为三员之一
     */
    public boolean isTripleAdmin(Long userId) {
        return isSystemAdmin(userId) || isSecurityAdmin(userId) || isAuditAdmin(userId);
    }

    /**
     * 获取用户的三员类型（如果不是三员返回null）
     */
    public AdminType getAdminType(Long userId) {
        if (isSystemAdmin(userId)) return AdminType.SYSTEM_ADMIN;
        if (isSecurityAdmin(userId)) return AdminType.SECURITY_ADMIN;
        if (isAuditAdmin(userId)) return AdminType.AUDIT_ADMIN;
        return null;
    }

    /**
     * 获取三员用户列表
     */
    public List<Map<String, Object>> getTripleAdminUsers(String adminType) {
        // 获取所有三员角色
        List<String> adminRoleKeys = Arrays.stream(AdminType.values())
                .map(AdminType::getRoleKey)
                .collect(Collectors.toList());
        
        // 如果指定了类型，只查该类型
        if (StringUtils.hasText(adminType)) {
            AdminType type = null;
            try {
                int code = Integer.parseInt(adminType);
                for (AdminType t : AdminType.values()) {
                    if (t.getCode() == code) {
                        type = t;
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                // 尝试按roleKey匹配
                type = AdminType.fromRoleKey(adminType);
            }
            if (type != null) {
                adminRoleKeys = Collections.singletonList(type.getRoleKey());
            }
        }
        
        // 查询三员角色
        List<Role> adminRoles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().in(Role::getRoleKey, adminRoleKeys)
        );
        
        if (adminRoles.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> adminRoleIds = adminRoles.stream().map(Role::getId).collect(Collectors.toList());
        
        // 查询拥有这些角色的用户
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, adminRoleIds)
        );
        
        Set<Long> userIds = userRoles.stream().map(UserRole::getUserId).collect(Collectors.toSet());
        
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 查询用户信息
        List<User> users = userMapper.selectBatchIds(userIds);
        
        // 构建返回结果
        Map<Long, AdminType> userAdminTypeMap = new HashMap<>();
        for (User user : users) {
            for (Role role : adminRoles) {
                UserRole ur = userRoles.stream()
                        .filter(r -> r.getUserId().equals(user.getId()) && r.getRoleId().equals(role.getId()))
                        .findFirst().orElse(null);
                if (ur != null) {
                    userAdminTypeMap.put(user.getId(), AdminType.fromRoleKey(role.getRoleKey()));
                    break;
                }
            }
        }
        
        return users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("realName", user.getRealName());
            map.put("status", user.getStatus());
            AdminType adminType2 = userAdminTypeMap.get(user.getId());
            if (adminType2 != null) {
                map.put("adminType", adminType2.getCode());
                map.put("adminTypeName", adminType2.getName());
                map.put("adminRoleKey", adminType2.getRoleKey());
            }
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 校验操作权限（三员之间不能互相操作）
     */
    public void validateTripleAdminOperation(Long operatorId, Long targetId) {
        // 如果操作者不是三员，允许操作（由其他权限系统控制）
        if (!isTripleAdmin(operatorId)) {
            return;
        }
        
        // 如果目标是普通用户（非三员），允许操作
        if (!isTripleAdmin(targetId)) {
            return;
        }
        
        // 三员之间不能互相操作
        throw new BusinessException(ErrorCode.ADMIN_CANNOT_OPERATE_OTHER);
    }

    /**
     * 校验不能删除自己的账号
     */
    public void validateCannotDeleteSelf(Long operatorId, Long targetId) {
        if (operatorId.equals(targetId)) {
            throw new BusinessException(ErrorCode.ADMIN_CANNOT_DELETE_SELF);
        }
    }

    /**
     * 获取三员操作审计日志
     */
    public List<OperationLog> getTripleAdminAuditLogs(Long userId, String module, 
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      int page, int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询三员的操作日志
        List<Long> adminUserIds = getTripleAdminUserIds();
        if (adminUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        wrapper.in(OperationLog::getUserId, adminUserIds);
        
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(module)) {
            wrapper.like(OperationLog::getModule, module);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getOperationTime, endTime);
        }
        
        wrapper.orderByDesc(OperationLog::getOperationTime);
        
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + size + " OFFSET " + offset);
        
        return operationLogMapper.selectList(wrapper);
    }

    /**
     * 初始化三员账号
     */
    public void initTripleAdmins() {
        log.info("[TripleAdminService] 开始初始化三员账号...");
        
        // 创建三员角色（如果不存在）
        for (AdminType adminType : AdminType.values()) {
            Role existingRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, adminType.getRoleKey())
            );
            if (existingRole == null) {
                Role role = new Role();
                role.setRoleKey(adminType.getRoleKey());
                role.setRoleName(adminType.getName());
                role.setRoleType(1); // 系统角色
                role.setStatus(1);
                role.setCreateTime(LocalDateTime.now());
                role.setUpdateTime(LocalDateTime.now());
                roleMapper.insert(role);
                log.info("[TripleAdminService] 创建三员角色: {}", adminType.getName());
            }
        }
        
        // 创建三员账号（如果不存在）
        createAdminIfNotExists("sys_admin", "系统管理员", AdminType.SYSTEM_ADMIN);
        createAdminIfNotExists("sec_admin", "安全管理员", AdminType.SECURITY_ADMIN);
        createAdminIfNotExists("audit_admin", "审计管理员", AdminType.AUDIT_ADMIN);
        
        log.info("[TripleAdminService] 三员账号初始化完成");
    }

    /**
     * 获取所有三员用户ID
     */
    private List<Long> getTripleAdminUserIds() {
        List<String> adminRoleKeys = Arrays.stream(AdminType.values())
                .map(AdminType::getRoleKey)
                .collect(Collectors.toList());
        
        List<Role> adminRoles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().in(Role::getRoleKey, adminRoleKeys)
        );
        
        if (adminRoles.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> adminRoleIds = adminRoles.stream().map(Role::getId).collect(Collectors.toList());
        
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, adminRoleIds)
        );
        
        return userRoles.stream().map(UserRole::getUserId).distinct().collect(Collectors.toList());
    }

    private boolean hasAdminRole(Long userId, AdminType adminType) {
        Role role = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, adminType.getRoleKey())
        );
        if (role == null) {
            return false;
        }
        
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, role.getId())
        );
        
        return userRole != null;
    }

    private void createAdminIfNotExists(String username, String realName, AdminType adminType) {
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        
        if (existingUser == null) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(AuthService.hashPassword("admin123")); // 默认密码
            user.setRealName(realName);
            user.setStatus(1);
            user.setSecurityLevel(4); // 最高密级
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
            
            // 分配角色
            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, adminType.getRoleKey())
            );
            if (role != null) {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(role.getId());
                userRoleMapper.insert(userRole);
            }
            
            log.info("[TripleAdminService] 创建三员账号: username={}, type={}", username, adminType.getName());
        }
    }
}
