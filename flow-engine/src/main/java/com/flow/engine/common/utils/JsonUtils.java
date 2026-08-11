package com.flow.engine.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON 序列化/反序列化工具（基于 Jackson）。
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private JsonUtils() {
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new com.flow.engine.common.exception.FlowException(500, "JSON 序列化失败: " + e.getMessage());
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new com.flow.engine.common.exception.FlowException(500, "JSON 反序列化失败: " + e.getMessage());
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new com.flow.engine.common.exception.FlowException(500, "JSON 反序列化失败: " + e.getMessage());
        }
    }

    public static <T> T fromJson(Object obj, Class<T> clazz) {
        return fromJson(toJson(obj), clazz);
    }

    /**
     * 宽容解析请求体为 Map：优先按 JSON 解析，失败则按 form-urlencoded 键值对解析。
     * 空请求体返回 null。用于兼容客户端以 application/x-www-form-urlencoded 提交的可选请求体，
     * 避免 @RequestBody Map 因媒体类型不匹配抛出 HttpMediaTypeNotSupportedException（500）。
     */
    public static Map<String, Object> parseBodyMapLoose(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                return MAPPER.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignore) {
                // JSON 解析失败时回退为表单解析
            }
        }
        Map<String, Object> map = new HashMap<>();
        for (String pair : trimmed.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            try {
                String k = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : URLDecoder.decode(pair, StandardCharsets.UTF_8);
                String v = idx > 0 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) : "";
                map.put(k, v);
            } catch (Exception ignore) {
                // 非法编码段跳过
            }
        }
        return map.isEmpty() ? null : map;
    }
}
