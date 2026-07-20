package com.flow.engine.common;

/**
 * 请求级上下文，基于 ThreadLocal 保存当前请求标识与用户信息。
 * 在 {@link RequestIdFilter} 中写入，请求结束后清除。
 */
public class RequestContext {

    private static final ThreadLocal<RequestContext> HOLDER = ThreadLocal.withInitial(RequestContext::new);

    private String requestId;
    private String userId;
    private String username;
    private String tenantId;

    public static RequestContext current() {
        return HOLDER.get();
    }

    public static void set(RequestContext ctx) {
        HOLDER.set(ctx);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static String fetchRequestId() {
        RequestContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getRequestId();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
