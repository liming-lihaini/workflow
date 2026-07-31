package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.entity.ApiToken;
import com.flow.engine.mapper.ApiTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人API Token服务
 * <p>
 * 用户可生成长期有效的 API Token，用于脚本/第三方系统以 Bearer 方式调用系统接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTokenService {

    private final ApiTokenMapper apiTokenMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** 单用户Token数量上限 */
    private static final int MAX_TOKENS_PER_USER = 10;

    /**
     * 生成Token（完整Token值仅在创建时返回一次）
     *
     * @param expireDays 有效天数，null 或 <=0 表示永久有效
     */
    public Map<String, Object> createToken(Long userId, String tokenName, Integer expireDays) {
        LambdaQueryWrapper<ApiToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiToken::getUserId, userId);
        if (apiTokenMapper.selectCount(wrapper) >= MAX_TOKENS_PER_USER) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Token数量已达上限" + MAX_TOKENS_PER_USER);
        }

        ApiToken token = new ApiToken();
        token.setUserId(userId);
        token.setTokenName(tokenName == null || tokenName.isBlank() ? "API Token" : tokenName.trim());
        token.setTokenValue(generateTokenValue());
        if (expireDays != null && expireDays > 0) {
            token.setExpireTime(LocalDateTime.now().plusDays(expireDays));
        }
        token.setCreateTime(LocalDateTime.now());
        apiTokenMapper.insert(token);

        log.info("生成API Token: userId={}, tokenName={}, id={}", userId, token.getTokenName(), token.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", token.getId());
        result.put("tokenName", token.getTokenName());
        result.put("tokenValue", token.getTokenValue());
        result.put("expireTime", token.getExpireTime() != null ? token.getExpireTime().format(FORMATTER) : null);
        result.put("createTime", token.getCreateTime().format(FORMATTER));
        return result;
    }

    /**
     * 查询用户的Token列表（Token值脱敏展示）
     */
    public List<Map<String, Object>> listTokens(Long userId) {
        LambdaQueryWrapper<ApiToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiToken::getUserId, userId).orderByDesc(ApiToken::getCreateTime);
        List<ApiToken> tokens = apiTokenMapper.selectList(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (ApiToken t : tokens) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("tokenName", t.getTokenName());
            m.put("tokenMasked", mask(t.getTokenValue()));
            m.put("expireTime", t.getExpireTime() != null ? t.getExpireTime().format(FORMATTER) : null);
            m.put("lastUsedTime", t.getLastUsedTime() != null ? t.getLastUsedTime().format(FORMATTER) : null);
            m.put("createTime", t.getCreateTime() != null ? t.getCreateTime().format(FORMATTER) : null);
            m.put("expired", t.getExpireTime() != null && LocalDateTime.now().isAfter(t.getExpireTime()));
            list.add(m);
        }
        return list;
    }

    /**
     * 删除Token（仅本人可删除）
     */
    public void deleteToken(Long userId, Long tokenId) {
        ApiToken token = apiTokenMapper.selectById(tokenId);
        if (token == null || !token.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Token不存在");
        }
        apiTokenMapper.deleteById(tokenId);
        log.info("删除API Token: userId={}, tokenId={}", userId, tokenId);
    }

    /**
     * 按Token值查找有效Token（未过期），供鉴权使用；命中时更新最近使用时间
     *
     * @return 有效Token，未找到或已过期返回 null
     */
    public ApiToken findValidToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<ApiToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiToken::getTokenValue, tokenValue);
        ApiToken token = apiTokenMapper.selectOne(wrapper);
        if (token == null) {
            return null;
        }
        if (token.getExpireTime() != null && LocalDateTime.now().isAfter(token.getExpireTime())) {
            return null;
        }
        token.setLastUsedTime(LocalDateTime.now());
        apiTokenMapper.updateById(token);
        return token;
    }

    /** Token值：ftk_ 前缀 + 40位十六进制随机串 */
    private String generateTokenValue() {
        StringBuilder sb = new StringBuilder("ftk_");
        for (int i = 0; i < 40; i++) {
            sb.append(HEX[RANDOM.nextInt(16)]);
        }
        return sb.toString();
    }

    /** 脱敏：保留前缀8位与末4位 */
    private String mask(String value) {
        if (value == null || value.length() < 16) {
            return "****";
        }
        return value.substring(0, 8) + "****" + value.substring(value.length() - 4);
    }
}
