package com.flow.engine.controllers;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 流程附件管理（ISSUE-021）：本地磁盘存储。
 * 上传后返回相对存储路径，流程变量中记录 {name, path, size}，办理节点凭 path 下载。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    /** 附件存储根目录（相对工程运行目录，可通过配置覆盖） */
    @Value("${flow.attachment.dir:upload/attachments}")
    private String baseDir;

    /** 单文件大小上限 20MB（与 spring.servlet.multipart 配置保持一致） */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    /** 合法存储路径：yyyyMMdd/uuid[.ext]，防止路径穿越 */
    private static final Pattern PATH_PATTERN =
            Pattern.compile("^\\d{8}/[a-f0-9\\-]{36}(\\.[A-Za-z0-9]{1,10})?$");

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件大小超过 20MB 限制");
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? Paths.get(file.getOriginalFilename()).getFileName().toString() : "unknown";
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < originalName.length() - 1) {
            String rawExt = originalName.substring(dotIdx + 1);
            if (rawExt.matches("[A-Za-z0-9]{1,10}")) {
                ext = "." + rawExt.toLowerCase();
            }
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String storedName = UUID.randomUUID() + ext;
        Path dir = Paths.get(baseDir, dateDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(storedName);
        file.transferTo(target.toAbsolutePath());

        String relativePath = dateDir + "/" + storedName;
        log.info("[Attachment] 上传成功: name={}, path={}, size={}", originalName, relativePath, file.getSize());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", originalName);
        data.put("path", relativePath);
        data.put("size", file.getSize());
        return Result.ok(data);
    }

    @GetMapping("/download")
    public void download(@RequestParam("path") String path,
                         @RequestParam(value = "name", required = false) String name,
                         HttpServletResponse response) throws IOException {
        if (!StringUtils.hasText(path) || !PATH_PATTERN.matcher(path).matches()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "非法的附件路径");
        }
        Path file = Paths.get(baseDir).resolve(path).normalize();
        if (!file.startsWith(Paths.get(baseDir).normalize()) || !Files.isRegularFile(file)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }

        String downloadName = StringUtils.hasText(name) ? name : file.getFileName().toString();
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(Files.size(file));
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedName);
        Files.copy(file, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 内联预览（供头像等 <img> 直连展示）：按扩展名返回图片 Content-Type，
     * 非图片类型回退为 octet-stream。路径校验规则与 download 一致。
     */
    @GetMapping("/preview")
    public void preview(@RequestParam("path") String path,
                        HttpServletResponse response) throws IOException {
        if (!StringUtils.hasText(path) || !PATH_PATTERN.matcher(path).matches()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "非法的附件路径");
        }
        Path file = Paths.get(baseDir).resolve(path).normalize();
        if (!file.startsWith(Paths.get(baseDir).normalize()) || !Files.isRegularFile(file)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }

        String fileName = file.getFileName().toString();
        String ext = "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < fileName.length() - 1) {
            ext = fileName.substring(dotIdx + 1).toLowerCase();
        }
        String contentType = switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(file));
        response.setHeader("Content-Disposition", "inline");
        Files.copy(file, response.getOutputStream());
        response.flushBuffer();
    }
}
