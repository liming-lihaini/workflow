package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.*;
import com.flow.engine.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 环境监测 LIMS - 共享实体 API（ISSUE-022，TRD §4.4）
 * 文件 / 预警 / 站内信 为跨模块共享实体
 */
@RestController
@RequestMapping("/api/v1/ems/shared")
@RequiredArgsConstructor
public class EmsSharedController {

    private final EmsFileMetaService fileMetaService;
    private final EmsAlertService alertService;
    private final EmsMessageService messageService;

    // ---------- 文件元信息（WORM，仅新增） ----------
    @PostMapping("/files")
    public Result<EmsFileMeta> archiveFile(@RequestBody EmsFileMeta f) {
        return Result.ok(fileMetaService.archive(f));
    }

    @GetMapping("/files")
    public Result<List<EmsFileMeta>> listFiles(@RequestParam(required = false) String bizType,
                                                @RequestParam(required = false) Long bizId) {
        if (bizType != null || bizId != null) {
            return Result.ok(fileMetaService.lambdaQuery()
                    .eq(bizType != null, EmsFileMeta::getBizType, bizType)
                    .eq(bizId != null, EmsFileMeta::getBizId, bizId).list());
        }
        return Result.ok(fileMetaService.list());
    }

    // ---------- 预警 ----------
    @PostMapping("/alerts")
    public Result<EmsAlert> pushAlert(@RequestBody EmsAlert a) {
        return Result.ok(alertService.push(a));
    }

    @GetMapping("/alerts")
    public Result<List<EmsAlert>> listAlerts(@RequestParam(required = false) Integer status) {
        if (status != null) {
            return Result.ok(alertService.lambdaQuery().eq(EmsAlert::getStatus, status).list());
        }
        return Result.ok(alertService.list());
    }

    // ---------- 站内信 ----------
    @PostMapping("/messages")
    public Result<EmsMessage> sendMessage(@RequestBody EmsMessage m) {
        return Result.ok(messageService.send(m));
    }

    @GetMapping("/messages")
    public Result<List<EmsMessage>> listMessages(@RequestParam(required = false) String toUser) {
        if (toUser != null) {
            return Result.ok(messageService.lambdaQuery().eq(EmsMessage::getToUser, toUser).list());
        }
        return Result.ok(messageService.list());
    }
}
