package com.flow.engine.common;

import com.flow.engine.entity.User;
import com.flow.engine.service.ApiTokenService;
import com.flow.engine.service.AuthService;
import com.flow.engine.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 统一身份认证过滤器（与具体业务接口解耦）。
 *
 * <p>在请求早期解析 {@code Authorization} 头，兼容两种凭证：
 * <ol>
 *   <li>会话 Token：{@code Bearer <sessionToken>}</li>
 *   <li>API Token：{@code Bearer <apiToken>}（{@code ftk_} 前缀）</li>
 * </ol>
 * 解析成功后将当前用户（userId / username）写入 {@link RequestContext}，
 * 业务 Controller 通过 {@code RequestContext.current()} 获取当前用户，
 * 无需再声明 {@code @RequestHeader("Authorization")}，后续切换认证方式只需在此调整。
 *
 * <p>注意：本过滤器仅"尽力"填充当前用户信息，不拦截未登录请求；
 * 是否需要登录由具体接口/拦截器决定。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 紧接 RequestIdFilter 之后
@RequiredArgsConstructor
public class AuthContextFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final ApiTokenService apiTokenService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String userId = null;
        String username = null;

        String authHeader = request.getHeader("Authorization");
        String token = extractBearerToken(authHeader);
        if (token != null) {
            // 1) 优先尝试会话 Token
            try {
                AuthService.SessionInfo session = authService.validateToken(token);
                if (session != null) {
                    userId = session.getUserId() != null ? String.valueOf(session.getUserId()) : null;
                    username = session.getUsername();
                }
            } catch (Exception ignored) {
                // 不是会话 Token，继续尝试 API Token
            }
            // 2) 会话 Token 失败时尝试 API Token
            if (userId == null) {
                try {
                    var apiToken = apiTokenService.findValidToken(token);
                    if (apiToken != null) {
                        userId = String.valueOf(apiToken.getUserId());
                        User user = userService.getUser(apiToken.getUserId());
                        username = user != null ? user.getUsername() : null;
                    }
                } catch (Exception ignored) {
                    // Token 无效，保留 userId/username 为 null
                }
            }
        }

        if (userId != null) {
            RequestContext ctx = RequestContext.current();
            ctx.setUserId(userId);
            ctx.setUsername(username);
        }

        chain.doFilter(request, response);
    }

    private String extractBearerToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader.trim();
    }
}
