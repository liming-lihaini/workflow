package com.flow.engine.aspect;

import com.flow.engine.annotation.OpLog;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.entity.OperationLog;
import com.flow.engine.service.AuthService;
import com.flow.engine.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面（ISSUE-014, TRD §6.3）
 * <p>
 * 自动记录标注了@OpLog的方法的操作日志，含前后数据。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final LogService logService;
    private final AuthService authService;

    /** ThreadLocal存储修改前数据 */
    private static final ThreadLocal<String> BEFORE_DATA_HOLDER = new ThreadLocal<>();

    @Around("@annotation(com.flow.engine.annotation.OpLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OpLog annotation = method.getAnnotation(OpLog.class);
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        
        try {
            // 执行目标方法
            result = point.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                recordOperationLog(point, annotation, result, error, startTime);
            } catch (Exception e) {
                log.warn("[OperationLogAspect] 记录操作日志失败: {}", e.getMessage());
            }
            // 清理ThreadLocal
            BEFORE_DATA_HOLDER.remove();
        }
    }

    private void recordOperationLog(ProceedingJoinPoint point, OpLog annotation, 
                                    Object result, Throwable error, long startTime) {
        OperationLog logEntity = new OperationLog();
        
        // 获取用户信息
        String token = extractToken();
        if (token != null) {
            try {
                AuthService.SessionInfo session = authService.validateToken(token);
                logEntity.setUserId(session.getUserId());
                logEntity.setUsername(session.getUsername());
            } catch (Exception e) {
                log.debug("Token验证失败，不记录用户信息: {}", e.getMessage());
            }
        }
        
        // 模块和操作
        logEntity.setModule(annotation.module());
        logEntity.setOperation(annotation.operation());
        
        // 方法信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        logEntity.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
        
        // 请求参数
        if (annotation.recordParams()) {
            try {
                Object[] args = point.getArgs();
                logEntity.setParams(JsonUtils.toJson(args));
            } catch (Exception e) {
                logEntity.setParams("序列化失败");
            }
        }
        
        // 返回结果
        if (annotation.recordResult() && result != null) {
            try {
                logEntity.setResult(JsonUtils.toJson(result));
            } catch (Exception e) {
                logEntity.setResult("序列化失败");
            }
        }
        
        // 修改前数据
        if (annotation.recordBeforeData()) {
            String beforeData = BEFORE_DATA_HOLDER.get();
            if (beforeData != null) {
                logEntity.setBeforeData(beforeData);
            }
        }
        
        // 修改后数据
        if (annotation.recordAfterData() && result != null) {
            try {
                logEntity.setAfterData(JsonUtils.toJson(result));
            } catch (Exception e) {
                logEntity.setAfterData("序列化失败");
            }
        }
        
        // IP
        logEntity.setIp(getClientIp());
        
        // 操作时间
        logEntity.setOperationTime(LocalDateTime.now());
        
        // 记录日志
        logService.recordOperationLog(logEntity);
        
        long costTime = System.currentTimeMillis() - startTime;
        log.debug("[OperationLogAspect] 记录操作日志: module={}, operation={}, user={}, cost={}ms", 
                annotation.module(), annotation.operation(), logEntity.getUsername(), costTime);
    }

    /**
     * 设置修改前数据（供Service层调用）
     */
    public static void setBeforeData(Object data) {
        if (data != null) {
            try {
                BEFORE_DATA_HOLDER.set(JsonUtils.toJson(data));
            } catch (Exception e) {
                log.warn("设置修改前数据失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 从请求头中提取Token
     */
    private String extractToken() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
