package com.flow.engine.interceptor;

import com.flow.engine.entity.AccessLog;
import com.flow.engine.service.AuthService;
import com.flow.engine.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * 访问日志拦截器（ISSUE-014, TRD §6.3）
 * <p>
 * 自动记录所有API请求的访问日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogInterceptor implements HandlerInterceptor {

    private final LogService logService;
    private final AuthService authService;

    private static final String START_TIME_ATTR = "accessLogStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录开始时间用于计算耗时
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        try {
            AccessLog accessLog = new AccessLog();
            
            // 获取用户信息（如果有Token）
            String token = extractToken(request);
            if (token != null) {
                try {
                    AuthService.SessionInfo session = authService.validateToken(token);
                    accessLog.setUserId(session.getUserId());
                    accessLog.setUsername(session.getUsername());
                } catch (Exception e) {
                    // Token无效，不记录用户信息
                    log.debug("Token验证失败，不记录用户信息: {}", e.getMessage());
                }
            }
            
            // 请求信息
            accessLog.setUrl(request.getRequestURI());
            accessLog.setMethod(request.getMethod());
            accessLog.setIp(getClientIp(request));
            accessLog.setUserAgent(request.getHeader("User-Agent"));
            
            // 请求参数（简化处理，不包含body）
            String queryString = request.getQueryString();
            if (queryString != null) {
                accessLog.setParams(queryString);
            }
            
            // 结果
            int status = response.getStatus();
            accessLog.setResult(status >= 200 && status < 300 ? 1 : 0);
            
            // 错误信息
            if (ex != null) {
                accessLog.setErrorMsg(ex.getMessage());
            } else if (status >= 400) {
                accessLog.setErrorMsg("HTTP " + status);
            }
            
            accessLog.setAccessTime(LocalDateTime.now());
            
            // 异步记录日志（避免影响主流程）
            logService.recordAccessLog(accessLog);
            
        } catch (Exception e) {
            log.warn("[AccessLogInterceptor] 记录访问日志失败: {}", e.getMessage());
        }
    }

    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
