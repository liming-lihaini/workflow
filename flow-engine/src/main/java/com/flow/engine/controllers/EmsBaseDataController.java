package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.common.BusinessException;
import com.flow.engine.dto.*;
import com.flow.engine.entity.*;
import com.flow.engine.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 环境监测 LIMS - 基础数据管理 API（ISSUE-022，TRD §5.1/§5.10）
 */
@RestController
@RequestMapping("/api/v1/ems/base")
@RequiredArgsConstructor
public class EmsBaseDataController {

    private final EmsCustomerService customerService;
    private final EmsMonitorPointService monitorPointService;
    private final EmsEntrustService entrustService;
    private final EmsDepartmentService departmentService;
    private final EmsIntegrationCfgService integrationCfgService;
    private final EmsVehicleService vehicleService;
    private final EmsSamplingOrderService samplingOrderService;
    private final EmsDispatchService dispatchService;
    private final EmsEmployeeService employeeService;
    private final EmsInstrumentService instrumentService;

    // ---------- 客户 ----------
    @PostMapping("/customers")
    public Result<EmsCustomer> createCustomer(@RequestBody EmsCustomer c) {
        return Result.ok(customerService.create(c));
    }

    @GetMapping("/customers")
    public Result<List<EmsCustomer>> listCustomers() {
        return Result.ok(customerService.list());
    }

    @PostMapping("/customers/{id}/disable")
    public Result<Void> disableCustomer(@PathVariable Long id) {
        customerService.disable(id);
        return Result.ok();
    }

    @PutMapping("/customers/{id}")
    public Result<EmsCustomer> updateCustomer(@PathVariable Long id, @RequestBody EmsCustomer c) {
        return Result.ok(customerService.update(id, c));
    }

    @PostMapping("/customers/batch-delete")
    public Result<Void> batchDeleteCustomers(@RequestBody List<Long> ids) {
        customerService.removeBatchByIds(ids);
        return Result.ok();
    }

    @PostMapping("/customers/import")
    public Result<Integer> importCustomers(@RequestParam("file") MultipartFile file) {
        return Result.ok(customerService.importByExcel(file));
    }

    @GetMapping("/customers/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("客户导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        List<EmsCustomerExcelVO> demo = new java.util.ArrayList<>();
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), EmsCustomerExcelVO.class)
                .sheet("客户")
                .doWrite(demo);
    }

    // ---------- 监测点位（已并入委托基础信息，不再独立维护） ----------

    // ---------- 委托草稿 ----------
    /** 创建/更新委托（含监测点位） */
    @PostMapping("/entrusts")
    public Result<com.flow.engine.dto.EmsEntrustVO> createEntrust(@RequestBody com.flow.engine.dto.EntrustSaveReq req) {
        return Result.ok(entrustService.saveWithPoints(req.getEntrust(), req.getPoints()));
    }

    @GetMapping("/entrusts")
    public Result<List<com.flow.engine.dto.EmsEntrustVO>> listEntrusts() {
        return Result.ok(entrustService.listVO());
    }

    /** 委托详情（含监测点位） */
    @GetMapping("/entrusts/{id}")
    public Result<com.flow.engine.dto.EmsEntrustVO> getEntrust(@PathVariable Long id) {
        return Result.ok(entrustService.getVO(id));
    }

    @PostMapping("/entrusts/{id}/submit")
    public Result<com.flow.engine.dto.EmsEntrustVO> submitEntrust(@PathVariable Long id, @RequestParam(required = false) String submitBy) {
        EmsEntrust e = entrustService.submit(id, submitBy);
        return Result.ok(entrustService.getVO(e.getId()));
    }

    @PostMapping("/entrusts/{id}/tech-confirm")
    public Result<com.flow.engine.dto.EmsEntrustVO> techConfirm(@PathVariable Long id,
                                          @RequestParam Long reviewerId,
                                          @RequestParam(required = false) String opinion) {
        EmsEntrust e = entrustService.techConfirm(id, reviewerId, opinion);
        return Result.ok(entrustService.getVO(e.getId()));
    }

    @PostMapping("/entrusts/{id}/reject")
    public Result<com.flow.engine.dto.EmsEntrustVO> rejectEntrust(@PathVariable Long id,
                                            @RequestParam Long reviewerId,
                                            @RequestParam String opinion) {
        EmsEntrust e = entrustService.reject(id, reviewerId, opinion);
        return Result.ok(entrustService.getVO(e.getId()));
    }

    // ---------- 部门 ----------
    @PostMapping("/departments")
    public Result<EmsDepartment> createDepartment(@RequestBody EmsDepartment d) {
        return Result.ok(departmentService.create(d));
    }

    @GetMapping("/departments")
    public Result<List<EmsDepartment>> listDepartments() {
        return Result.ok(departmentService.list());
    }

    // ---------- 集成配置 ----------
    @PostMapping("/integration-cfg")
    public Result<EmsIntegrationCfg> upsertCfg(@RequestBody EmsIntegrationCfg cfg) {
        return Result.ok(integrationCfgService.upsert(cfg));
    }

    @GetMapping("/integration-cfg/{key}/plain")
    public Result<String> getCfgPlain(@PathVariable String key) {
        return Result.ok(integrationCfgService.getDecrypted(key));
    }

    // ---------- 车辆（4.4 车辆台账） ----------
    @PostMapping("/vehicles")
    public Result<EmsVehicle> createVehicle(@RequestBody EmsVehicle v) {
        return Result.ok(vehicleService.create(v));
    }

    @PutMapping("/vehicles/{id}")
    public Result<EmsVehicle> updateVehicle(@PathVariable Long id, @RequestBody EmsVehicle v) {
        return Result.ok(vehicleService.updateVehicle(id, v));
    }

    @DeleteMapping("/vehicles/{id}")
    public Result<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/vehicles")
    public Result<?> listVehicles(@RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(vehicleService.pageSearch(keyword, page, size));
    }

    // ---------- 采样订单（5.1+5.2） ----------
    @PostMapping("/sampling-order/gen")
    public Result<Integer> genOrders(@RequestParam Long entrustId) {
        EmsEntrust e = entrustService.getById(entrustId);
        if (e == null) {
            return Result.fail(400, "委托不存在");
        }
        return Result.ok(samplingOrderService.genFromEntrust(e));
    }

    @GetMapping("/sampling-orders")
    public Result<List<EmsSamplingOrder>> listSamplingOrders(@RequestParam(required = false) String status) {
        return Result.ok(samplingOrderService.listByStatus(status));
    }

    // ---------- 调度派单（5.2） ----------
    private java.time.LocalDateTime parseLocalDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // 兼容 "2026-08-02T09:00:00" 与 "2026-08-02 09:00:00" 两种格式
        String iso = text.replace(' ', 'T');
        return java.time.LocalDateTime.parse(iso);
    }

    @PostMapping("/dispatch")
    public Result<EmsDispatch> createDispatch(@RequestParam Long orderId,
                                              @RequestParam(required = false) Long vehicleId,
                                              @RequestParam(required = false) Long leadId,
                                              @RequestParam(required = false) List<Long> empIds,
                                              @RequestParam(required = false) List<Long> instrumentIds,
                                              @RequestParam(required = false) String planStart,
                                              @RequestParam(required = false) String planEnd,
                                              @RequestParam(required = false) String note) {
        java.time.LocalDateTime ps = planStart == null ? null : parseLocalDateTime(planStart);
        java.time.LocalDateTime pe = planEnd == null ? null : parseLocalDateTime(planEnd);
        return Result.ok(dispatchService.dispatch(orderId, vehicleId, leadId, empIds, instrumentIds, ps, pe, note));
    }

    @GetMapping("/dispatch/check")
    public Result<Boolean> checkConflict(@RequestParam String planStart,
                                         @RequestParam String planEnd,
                                         @RequestParam List<Long> resourceIds) {
        boolean conflict = dispatchService.hasConflict(
                parseLocalDateTime(planStart),
                parseLocalDateTime(planEnd),
                resourceIds);
        return Result.ok(conflict);
    }

    @GetMapping("/dispatch")
    public Result<EmsDispatchDetailVO> getDispatchDetail(@RequestParam Long orderId) {
        return Result.ok(dispatchService.getDispatchDetail(orderId));
    }

    // ---------- 派单资源：人员/设备 ----------
    @GetMapping("/employees")
    public Result<List<EmsEmployee>> listEmployees() {
        return Result.ok(employeeService.list());
    }

    @PostMapping("/employees")
    public Result<EmsEmployee> createEmployee(@RequestBody EmsEmployee e) {
        e.setCreateTime(java.time.LocalDateTime.now());
        e.setUpdateTime(java.time.LocalDateTime.now());
        employeeService.save(e);
        return Result.ok(e);
    }

    @GetMapping("/instruments")
    public Result<?> listInstruments(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return Result.ok(instrumentService.pageSearch(keyword, status, page, size));
    }

    @PostMapping("/instruments")
    public Result<EmsInstrument> createInstrument(@RequestBody EmsInstrument i) {
        return Result.ok(instrumentService.create(i));
    }

    @PutMapping("/instruments/{id}")
    public Result<EmsInstrument> updateInstrument(@PathVariable Long id, @RequestBody EmsInstrument i) {
        return Result.ok(instrumentService.updateInstrument(id, i));
    }

    @DeleteMapping("/instruments/{id}")
    public Result<Void> deleteInstrument(@PathVariable Long id) {
        instrumentService.removeById(id);
        return Result.ok();
    }

    /** 校准登记（TRD 5.5.5） */
    @PostMapping("/instruments/{id}/calibrate")
    public Result<EmsInstrument> calibrateInstrument(@PathVariable Long id,
                                                     @RequestParam(required = false) String calibDate,
                                                     @RequestParam(required = false) String calibDue,
                                                     @RequestParam(required = false) String certNo) {
        java.time.LocalDate cd = calibDate == null ? null : java.time.LocalDate.parse(calibDate);
        java.time.LocalDate cud = calibDue == null ? null : java.time.LocalDate.parse(calibDue);
        return Result.ok(instrumentService.calibrate(id, cd, cud, certNo));
    }

    /** 设备详情（TRD 5.5）：基本信息 + 校准记录 + 关联采样任务 */
    @GetMapping("/instruments/{id}/detail")
    public Result<EmsInstrumentDetailVO> getInstrumentDetail(@PathVariable Long id) {
        EmsInstrumentDetailVO vo = instrumentService.getDetail(id);
        if (vo == null) throw new BusinessException("设备不存在: " + id);
        return Result.ok(vo);
    }

    /** 校准到期预警列表（TRD 5.5.2 三级预警） */
    @GetMapping("/instruments/expiring")
    public Result<List<EmsInstrument>> expiringInstruments() {
        return Result.ok(instrumentService.expiringSoon());
    }
}
