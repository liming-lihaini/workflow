package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsDispatchDevice;
import com.flow.engine.mapper.EmsDispatchDeviceMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmsDispatchDeviceService extends ServiceImpl<EmsDispatchDeviceMapper, EmsDispatchDevice> {

    public List<EmsDispatchDevice> listByDispatch(Long dispatchId) {
        return this.list(new LambdaQueryWrapper<EmsDispatchDevice>().eq(EmsDispatchDevice::getDispatchId, dispatchId));
    }
}
