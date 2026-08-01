package com.flow.engine.service;

import com.flow.engine.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 环境监测 LIMS 基础数据服务测试（ISSUE-022）
 * 覆盖 BR-022 业务规则与 CRUD
 */
@SpringBootTest
@DisplayName("环境监测基础数据服务测试")
public class EmsBaseDataServiceTest {

    @Autowired
    private EmsCustomerService customerService;
    @Autowired
    private EmsMonitorPointService monitorPointService;
    @Autowired
    private EmsEntrustService entrustService;
    @Autowired
    private EmsDepartmentService departmentService;
    @Autowired
    private EmsIntegrationCfgService integrationCfgService;
    @Autowired
    private EmsVehicleService vehicleService;

    @Test
    @DisplayName("BR-022-02 客户名不能为空")
    void testCustomerNameRequired() {
        EmsCustomer c = new EmsCustomer();
        assertThrows(IllegalArgumentException.class, () -> customerService.create(c));
    }

    @Test
    @DisplayName("BR-022-01 客户名+信用代码唯一")
    void testCustomerUnique() {
        EmsCustomer c = new EmsCustomer();
        c.setCustName("唯一样企" + System.currentTimeMillis());
        c.setCreditCode("91110000XX");
        customerService.create(c);
        EmsCustomer dup = new EmsCustomer();
        dup.setCustName(c.getCustName());
        dup.setCreditCode(c.getCreditCode());
        assertThrows(IllegalArgumentException.class, () -> customerService.create(dup));
    }

    @Test
    @DisplayName("客户停用 status=0")
    void testCustomerDisable() {
        EmsCustomer c = new EmsCustomer();
        c.setCustName("停用客户" + System.currentTimeMillis());
        c.setCreditCode("91110000YY");
        EmsCustomer created = customerService.create(c);
        customerService.disable(created.getId());
        EmsCustomer after = customerService.getById(created.getId());
        assertEquals(0, after.getStatus());
    }

    @Test
    @DisplayName("BR-022-03 点位名不能为空")
    void testPointNameRequired() {
        assertThrows(IllegalArgumentException.class, () -> monitorPointService.create(new EmsMonitorPoint()));
    }

    @Test
    @DisplayName("BR-022-04 经纬度范围校验")
    void testPointLngLatRange() {
        EmsMonitorPoint p = new EmsMonitorPoint();
        p.setPointName("超限点位" + System.currentTimeMillis());
        p.setLng(200.0);
        assertThrows(IllegalArgumentException.class, () -> monitorPointService.create(p));
    }

    @Test
    @DisplayName("点位创建写入历史超标默认0")
    void testPointDefaultFlag() {
        EmsMonitorPoint p = new EmsMonitorPoint();
        p.setPointName("默认点位" + System.currentTimeMillis());
        p.setLng(116.39);
        p.setLat(39.91);
        EmsMonitorPoint created = monitorPointService.create(p);
        assertEquals(0, created.getHistoryOverFlag());
    }

    @Test
    @DisplayName("BR-022-05 委托名不能为空")
    void testEntrustNameRequired() {
        assertThrows(IllegalArgumentException.class, () -> entrustService.createDraft(new EmsEntrust()));
    }

    @Test
    @DisplayName("委托草稿默认状态=草稿")
    void testEntrustDraftStatus() {
        EmsEntrust e = new EmsEntrust();
        e.setEntrustName("草稿委托" + System.currentTimeMillis());
        EmsEntrust created = entrustService.createDraft(e);
        assertEquals("草稿", created.getStatus());
    }

    @Test
    @DisplayName("BR-022-07 部门名不能为空")
    void testDeptNameRequired() {
        assertThrows(IllegalArgumentException.class, () -> departmentService.create(new EmsDepartment()));
    }

    @Test
    @DisplayName("BR-022-08 父部门不存在报错")
    void testDeptParentNotExist() {
        EmsDepartment d = new EmsDepartment();
        d.setDeptName("子部门" + System.currentTimeMillis());
        d.setParentId(999999L);
        assertThrows(IllegalArgumentException.class, () -> departmentService.create(d));
    }

    @Test
    @DisplayName("BR-022-11 集成配置密钥加解密闭环")
    void testIntegrationCfgEncrypt() {
        EmsIntegrationCfg cfg = new EmsIntegrationCfg();
        cfg.setCfgKey("SMS_SECRET_" + System.currentTimeMillis());
        cfg.setCfgValue("plain-secret-123");
        cfg.setEncryptFlag(1);
        integrationCfgService.upsert(cfg);
        String plain = integrationCfgService.getDecrypted(cfg.getCfgKey());
        assertEquals("plain-secret-123", plain);
    }

    @Test
    @DisplayName("车辆创建默认可用 status=1")
    void testVehicleCreate() {
        EmsVehicle v = new EmsVehicle();
        v.setPlateNo("京A" + System.currentTimeMillis() % 100000);
        EmsVehicle created = vehicleService.create(v);
        assertEquals(1, created.getStatus());
    }
}
