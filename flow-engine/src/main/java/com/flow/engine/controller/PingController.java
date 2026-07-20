package com.flow.engine.controller;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 脚手架自测控制器，用于验证统一响应与全局异常处理链路。
 */
@RestController
@RequestMapping("/api/ping")
public class PingController {

    @GetMapping
    public Result<String> ping() {
        return Result.ok("pong");
    }

    @GetMapping("/fail")
    public Result<Void> fail() {
        throw new BusinessException("演示业务异常");
    }

    @GetMapping("/boom")
    public Result<Void> boom() {
        throw new RuntimeException("演示系统异常");
    }
}
