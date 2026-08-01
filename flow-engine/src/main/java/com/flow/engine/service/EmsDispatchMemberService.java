package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsDispatchMember;
import com.flow.engine.mapper.EmsDispatchMemberMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmsDispatchMemberService extends ServiceImpl<EmsDispatchMemberMapper, EmsDispatchMember> {

    public List<EmsDispatchMember> listByDispatch(Long dispatchId) {
        return this.list(new LambdaQueryWrapper<EmsDispatchMember>().eq(EmsDispatchMember::getDispatchId, dispatchId));
    }
}
