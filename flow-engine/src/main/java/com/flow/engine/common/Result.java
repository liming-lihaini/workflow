package com.flow.engine.common;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应包装。所有 REST 接口返回体均为此结构（TRD §3）。
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private String requestId;
    private LocalDateTime timestamp;

    public static <T> Result<T> ok(T data) {
        return of(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return of(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return of(errorCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return of(code, message, null);
    }

    private static <T> Result<T> of(int code, String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        r.setRequestId(RequestContext.fetchRequestId());
        r.setTimestamp(LocalDateTime.now());
        return r;
    }
}
