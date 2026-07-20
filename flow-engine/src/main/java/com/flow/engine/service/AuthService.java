package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务（ISSUE-013）
 * <p>
 * 基于 Token + 内存会话的简单认证机制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    /** 内存会话：token -> SessionInfo */
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /** Token 有效期（分钟） */
    private static final int TOKEN_EXPIRE_MINUTES = 60;

    /**
     * 用户登录
     */
    public String login(String username, String password) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 简单密码校验（实际应使用 BCrypt）
        if (!hashPassword(password).equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "用户已停用");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        SessionInfo session = new SessionInfo();
        session.setUserId(user.getId());
        session.setUsername(user.getUsername());
        session.setToken(token);
        session.setCreateTime(LocalDateTime.now());
        session.setExpireTime(LocalDateTime.now().plusMinutes(TOKEN_EXPIRE_MINUTES));
        sessions.put(token, session);

        log.info("用户登录: username={}, token={}", username, token);
        return token;
    }

    /**
     * 注销
     */
    public void logout(String token) {
        sessions.remove(token);
        log.info("用户注销: token={}", token);
    }

    /**
     * 验证Token并返回会话信息
     */
    public SessionInfo validateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        SessionInfo session = sessions.get(token);
        if (session == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        if (LocalDateTime.now().isAfter(session.getExpireTime())) {
            sessions.remove(token);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        return session;
    }

    /**
     * 获取当前用户ID
     */
    public Long getCurrentUserId(String token) {
        return validateToken(token).getUserId();
    }

    /**
     * 密码哈希（简单SHA-256，生产环境应使用BCrypt）
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 会话信息
     */
    @lombok.Data
    public static class SessionInfo {
        private Long userId;
        private String username;
        private String token;
        private LocalDateTime createTime;
        private LocalDateTime expireTime;
    }
}
