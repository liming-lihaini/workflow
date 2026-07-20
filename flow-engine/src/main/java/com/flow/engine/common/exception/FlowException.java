package com.flow.engine.common.exception;

import com.flow.engine.common.ErrorCode;
import lombok.Getter;

/**
 * 流程引擎统一异常。框架与业务节点均抛出此异常，由全局异常处理器转换为 {@link com.flow.engine.common.Result}。
 */
@Getter
public class FlowException extends RuntimeException {

    private final int code;

    public FlowException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }

    public FlowException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public FlowException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public FlowException(int code, String message) {
        super(message);
        this.code = code;
    }
}
