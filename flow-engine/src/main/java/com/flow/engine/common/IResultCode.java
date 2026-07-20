package com.flow.engine.common;

/**
 * 统一结果码接口（TRD §8.2）
 */
public interface IResultCode {

    int getCode();

    String getMessage();
}
