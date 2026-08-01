package com.flow.engine.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsFileMeta;
import com.flow.engine.mapper.EmsFileMetaMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 环境监测 - 文件元信息服务（TRD 4.4 共享实体，WORM 受控：仅新增不可改删）
 */
@Service
public class EmsFileMetaService extends ServiceImpl<EmsFileMetaMapper, EmsFileMeta> {

    public EmsFileMeta archive(EmsFileMeta f) {
        f.setCreateTime(LocalDateTime.now());
        this.save(f);
        return f;
    }
}
