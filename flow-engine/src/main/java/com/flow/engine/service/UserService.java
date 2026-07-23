package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.entity.Dept;
import com.flow.engine.entity.User;
import com.flow.engine.entity.UserPost;
import com.flow.engine.mapper.DeptMapper;
import com.flow.engine.mapper.UserMapper;
import com.flow.engine.mapper.UserPostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务（ISSUE-013）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserPostMapper userPostMapper;
    private final DeptMapper deptMapper;

    /**
     * 创建用户（密码加密存储）
     */
    public User createUser(User user) {
        // 校验用户名唯一
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }
        user.setStatus(1);
        // 密码加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(AuthService.hashPassword(user.getPassword()));
        }
        if (user.getSecurityLevel() == null) {
            user.setSecurityLevel(1); // 默认公开
        }
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 创建主部门兼职记录
        if (user.getDeptId() != null) {
            UserPost userPost = new UserPost();
            userPost.setUserId(user.getId());
            userPost.setDeptId(user.getDeptId());
            userPost.setPostId(user.getPostId());
            userPost.setIsMain(1);
            userPostMapper.insert(userPost);
        }

        log.info("创建用户: id={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    /**
     * 获取用户详情
     */
    public User getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    /**
     * 获取用户列表
     */
    public List<User> listUsers(Long deptId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(User::getDeptId, deptId);
        }
        return userMapper.selectList(wrapper);
    }

    /**
     * 分页查询用户（支持关键字搜索）
     */
    public java.util.Map<String, Object> listUsersPage(Long deptId, String keyword, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(User::getDeptId, deptId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result =
                userMapper.selectPage(pageParam, wrapper);

        // 查询部门名称映射
        java.util.Map<Long, String> deptNameMap = new java.util.HashMap<>();
        List<Long> deptIds = result.getRecords().stream()
                .map(User::getDeptId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!deptIds.isEmpty()) {
            List<Dept> depts = deptMapper.selectBatchIds(deptIds);
            for (Dept dept : depts) {
                deptNameMap.put(dept.getId(), dept.getDeptName());
            }
        }

        // 构建含deptName的响应
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        for (User user : result.getRecords()) {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("realName", user.getRealName());
            item.put("email", user.getEmail());
            item.put("phone", user.getPhone());
            item.put("deptId", user.getDeptId());
            item.put("deptName", deptNameMap.getOrDefault(user.getDeptId(), ""));
            item.put("postId", user.getPostId());
            item.put("securityLevel", user.getSecurityLevel());
            item.put("status", user.getStatus());
            item.put("createTime", user.getCreateTime());
            item.put("updateTime", user.getUpdateTime());
            list.add(item);
        }

        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("list", list);
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    /**
     * 更新用户
     */
    public User updateUser(Long id, User user) {
        User existing = getUser(id);
        if (user.getRealName() != null) existing.setRealName(user.getRealName());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getDeptId() != null) existing.setDeptId(user.getDeptId());
        if (user.getPostId() != null) existing.setPostId(user.getPostId());
        if (user.getSecurityLevel() != null) existing.setSecurityLevel(user.getSecurityLevel());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
        log.info("更新用户: id={}", id);
        return existing;
    }

    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        getUser(id);
        userMapper.deleteById(id);
        // 删除兼职记录
        userPostMapper.delete(new LambdaQueryWrapper<UserPost>().eq(UserPost::getUserId, id));
        log.info("删除用户: id={}", id);
    }

    /**
     * 重置密码（加密存储）
     */
    public void resetPassword(Long id, String newPassword) {
        User user = getUser(id);
        user.setPassword(AuthService.hashPassword(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("重置密码: id={}", id);
    }

    /**
     * 设置密级
     */
    public void setSecurityLevel(Long userId, Integer securityLevel) {
        User user = getUser(userId);
        user.setSecurityLevel(securityLevel);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("设置密级: userId={}, level={}", userId, securityLevel);
    }

    /**
     * 添加兼职
     */
    public UserPost addUserPost(Long userId, Long deptId, Long postId, boolean isMain) {
        getUser(userId); // 校验用户存在
        UserPost userPost = new UserPost();
        userPost.setUserId(userId);
        userPost.setDeptId(deptId);
        userPost.setPostId(postId);
        userPost.setIsMain(isMain ? 1 : 0);
        userPostMapper.insert(userPost);
        log.info("添加兼职: userId={}, deptId={}, isMain={}", userId, deptId, isMain);
        return userPost;
    }

    /**
     * 删除兼职
     */
    public void deleteUserPost(Long userId, Long postId) {
        userPostMapper.delete(new LambdaQueryWrapper<UserPost>()
                .eq(UserPost::getUserId, userId)
                .eq(UserPost::getPostId, postId)
                .eq(UserPost::getIsMain, 0)); // 不能删主部门
        log.info("删除兼职: userId={}, postId={}", userId, postId);
    }

    /**
     * 获取用户兼职信息
     */
    public List<UserPost> getUserPosts(Long userId) {
        return userPostMapper.selectList(
                new LambdaQueryWrapper<UserPost>().eq(UserPost::getUserId, userId));
    }

    /**
     * 获取用户所有部门（主部门+兼职）
     */
    public List<Long> getUserDeptIds(Long userId) {
        List<UserPost> posts = getUserPosts(userId);
        return posts.stream().map(UserPost::getDeptId).collect(Collectors.toList());
    }

    /**
     * 获取用户可访问的部门列表（含兼职）
     */
    public List<Long> getAccessibleDepts(Long userId) {
        return getUserDeptIds(userId);
    }
}
