package com.flow.engine.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsEmployee;
import com.flow.engine.mapper.EmsEmployeeMapper;
import org.springframework.stereotype.Service;

@Service
public class EmsEmployeeService extends ServiceImpl<EmsEmployeeMapper, EmsEmployee> {
}
