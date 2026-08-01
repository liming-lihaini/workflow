package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsIntegrationCfg;
import com.flow.engine.mapper.EmsIntegrationCfgMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 环境监测 - 集成配置服务（TRD 5.10，密钥 AES 密文）
 * 业务规则：BR-022-11 cfgKey 唯一；encryptFlag=1 时明文入库前先 AES 加密，读取按需解密
 */
@Service
public class EmsIntegrationCfgService extends ServiceImpl<EmsIntegrationCfgMapper, EmsIntegrationCfg> {

    // AES-128：密钥必须为 16 字节（"emsintegrate2018" 恰好 16 字符）
    private static final String KEY = "emsintegrate2018";
    private static final String ALG = "AES/ECB/PKCS5Padding";

    public EmsIntegrationCfg upsert(EmsIntegrationCfg cfg) {
        if (!StringUtils.hasText(cfg.getCfgKey())) {
            throw new IllegalArgumentException("配置键不能为空(BR-022-11)");
        }
        if (cfg.getEncryptFlag() != null && cfg.getEncryptFlag() == 1 && StringUtils.hasText(cfg.getCfgValue())) {
            cfg.setCfgValue(encrypt(cfg.getCfgValue()));
        }
        cfg.setCreateTime(LocalDateTime.now());
        cfg.setUpdateTime(LocalDateTime.now());
        EmsIntegrationCfg exist = this.getOne(new LambdaQueryWrapper<EmsIntegrationCfg>()
                .eq(EmsIntegrationCfg::getCfgKey, cfg.getCfgKey()));
        if (exist != null) {
            cfg.setId(exist.getId());
            this.updateById(cfg);
        } else {
            this.save(cfg);
        }
        return cfg;
    }

    public String getDecrypted(String cfgKey) {
        EmsIntegrationCfg cfg = this.getOne(new LambdaQueryWrapper<EmsIntegrationCfg>()
                .eq(EmsIntegrationCfg::getCfgKey, cfgKey));
        if (cfg == null || !StringUtils.hasText(cfg.getCfgValue())) {
            return null;
        }
        if (cfg.getEncryptFlag() != null && cfg.getEncryptFlag() == 1) {
            return decrypt(cfg.getCfgValue());
        }
        return cfg.getCfgValue();
    }

    private String encrypt(String plain) {
        try {
            Cipher c = Cipher.getInstance(ALG);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY.getBytes(), "AES"));
            return Base64.getEncoder().encodeToString(c.doFinal(plain.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    private String decrypt(String cipher) {
        try {
            Cipher c = Cipher.getInstance(ALG);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY.getBytes(), "AES"));
            return new String(c.doFinal(Base64.getDecoder().decode(cipher)));
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }
}
