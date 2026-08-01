package com.flow.engine.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flow.engine.entity.EmsCustomer;
import com.flow.engine.entity.EmsCustomerExcelVO;
import com.flow.engine.mapper.EmsCustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 环境监测 - 客户档案服务（TRD 5.1）
 * 业务规则：BR-022-01 客户名+信用代码唯一；BR-022-02 名称必填；BR-022-10 停用的客户不可被新委托引用（由 ISSUE-023 校验）
 */
@Service
public class EmsCustomerService extends ServiceImpl<EmsCustomerMapper, EmsCustomer> {

    public boolean existsByUnique(String custName, String creditCode) {
        return this.count(new LambdaQueryWrapper<EmsCustomer>()
                .eq(EmsCustomer::getCustName, custName)
                .eq(EmsCustomer::getCreditCode, creditCode)) > 0;
    }

    public EmsCustomer create(EmsCustomer c) {
        if (!StringUtils.hasText(c.getCustName())) {
            throw new IllegalArgumentException("客户名称不能为空(BR-022-02)");
        }
        if (existsByUnique(c.getCustName(), c.getCreditCode())) {
            throw new IllegalArgumentException("客户名称+信用代码已存在(BR-022-01)");
        }
        c.setStatus(c.getStatus() == null ? 1 : c.getStatus());
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        this.save(c);
        return c;
    }

    public void disable(Long id) {
        EmsCustomer c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        c.setStatus(0);
        c.setUpdateTime(LocalDateTime.now());
        this.updateById(c);
    }

    public EmsCustomer update(Long id, EmsCustomer src) {
        EmsCustomer c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        if (!StringUtils.hasText(src.getCustName())) {
            throw new IllegalArgumentException("客户名称不能为空(BR-022-02)");
        }
        // 名称或信用代码变更时校验唯一性
        boolean nameChanged = !c.getCustName().equals(src.getCustName());
        boolean codeChanged = !StringUtils.hasText(c.getCreditCode()) && StringUtils.hasText(src.getCreditCode())
                || StringUtils.hasText(c.getCreditCode()) && !StringUtils.hasText(src.getCreditCode())
                || (StringUtils.hasText(c.getCreditCode()) && StringUtils.hasText(src.getCreditCode())
                    && !c.getCreditCode().equals(src.getCreditCode()));
        if (nameChanged || codeChanged) {
            if (existsByUnique(src.getCustName(), src.getCreditCode())) {
                throw new IllegalArgumentException("客户名称+信用代码已存在(BR-022-01)");
            }
        }
        c.setCustName(src.getCustName());
        c.setCreditCode(src.getCreditCode());
        c.setContact(src.getContact());
        c.setTel(src.getTel());
        c.setInvoiceTitle(src.getInvoiceTitle());
        c.setTaxNo(src.getTaxNo());
        c.setCity(src.getCity());
        c.setAddress(src.getAddress());
        c.setStatus(src.getStatus() == null ? c.getStatus() : src.getStatus());
        c.setUpdateTime(LocalDateTime.now());
        this.updateById(c);
        return c;
    }

    public void removeBatchByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        this.removeByIds(ids);
    }

    /**
     * 从 Excel 批量导入客户。
     * 跳过与库内已存在的(客户名称+信用代码)重复行，跳过必填为空的行。
     * @return 实际新增条数
     */
    public int importByExcel(MultipartFile file) {
        List<EmsCustomerExcelVO> rows = readExcel(file);
        List<EmsCustomer> toSave = new ArrayList<>();
        for (EmsCustomerExcelVO vo : rows) {
            if (!StringUtils.hasText(vo.getCustName())) {
                continue;
            }
            if (existsByUnique(vo.getCustName(), vo.getCreditCode())) {
                continue;
            }
            EmsCustomer c = new EmsCustomer();
            c.setCustName(vo.getCustName());
            c.setCreditCode(vo.getCreditCode());
            c.setContact(vo.getContact());
            c.setTel(vo.getTel());
            c.setInvoiceTitle(vo.getInvoiceTitle());
            c.setTaxNo(vo.getTaxNo());
            c.setCity(vo.getCity());
            c.setAddress(vo.getAddress());
            c.setStatus(1);
            c.setCreateTime(LocalDateTime.now());
            c.setUpdateTime(LocalDateTime.now());
            toSave.add(c);
        }
        if (!toSave.isEmpty()) {
            this.saveBatch(toSave);
        }
        return toSave.size();
    }

    private List<EmsCustomerExcelVO> readExcel(MultipartFile file) {
        List<EmsCustomerExcelVO> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), EmsCustomerExcelVO.class, new ReadListener<EmsCustomerExcelVO>() {
                @Override
                public void invoke(EmsCustomerExcelVO data, AnalysisContext context) {
                    if (data != null) {
                        rows.add(data);
                    }
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // no-op
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new IllegalArgumentException("Excel 解析失败：" + e.getMessage(), e);
        }
        return rows;
    }
}
