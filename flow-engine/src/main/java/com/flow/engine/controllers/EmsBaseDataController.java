package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
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
import java.util.Map;

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
    private final EmsSamplingService samplingService;
    private final UserService userService;
    private final EmsDispatchService dispatchService;
    private final EmsEmployeeService employeeService;
    private final EmsInstrumentService instrumentService;
    private final EmsSampleParamConfigService sampleParamConfigService;

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

    /** 客户详情：基本信息 + 检测委托清单 */
    @GetMapping("/customers/{id}/detail")
    public Result<Map<String, Object>> getCustomerDetail(@PathVariable Long id) {
        EmsCustomer customer = customerService.getById(id);
        if (customer == null) {
            return Result.fail(404, "客户不存在");
        }
        List<com.flow.engine.dto.EmsEntrustVO> entrusts = entrustService.listVOByCustId(id);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("customer", customer);
        data.put("entrusts", entrusts);
        return Result.ok(data);
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
        // 当前用户由统一认证过滤器(AuthContextFilter)解析后写入 RequestContext
        User operator = currentUser();
        return Result.ok(entrustService.saveWithPoints(req.getEntrust(), req.getPoints(), operator));
    }

    @GetMapping("/entrusts")
    public Result<List<com.flow.engine.dto.EmsEntrustVO>> listEntrusts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<com.flow.engine.dto.EmsEntrustVO> list = entrustService.listVO(status);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            list = list.stream()
                    .filter(v -> (v.getEntrustName() != null && v.getEntrustName().contains(k))
                            || (v.getEntrustNo() != null && v.getEntrustNo().contains(k))
                            || (v.getCustName() != null && v.getCustName().contains(k)))
                    .collect(java.util.stream.Collectors.toList());
        }
        return Result.ok(list);
    }

    /** 批量删除委托（ISSUE-026） */
    @PostMapping("/entrusts/batch-delete")
    public Result<Void> batchDeleteEntrusts(@RequestBody List<Long> ids) {
        entrustService.batchDelete(ids);
        return Result.ok();
    }

    /** 委托详情（含监测点位） */
    @GetMapping("/entrusts/{id}")
    public Result<com.flow.engine.dto.EmsEntrustVO> getEntrust(@PathVariable Long id) {
        return Result.ok(entrustService.getVO(id));
    }

    /** 委托操作历史（详情页「操作记录」，按时间倒序） */
    @GetMapping("/entrusts/{id}/history")
    public Result<List<com.flow.engine.entity.EmsEntrustHistory>> getEntrustHistory(@PathVariable Long id) {
        return Result.ok(entrustService.listHistory(id));
    }

    @PostMapping("/entrusts/{id}/submit")
    public Result<com.flow.engine.dto.EmsEntrustVO> submitEntrust(@PathVariable Long id, @RequestParam(required = false) String submitBy) {
        // 前端未传或传占位值时，回退为当前登录用户，保证操作历史的操作人准确
        if (submitBy == null || submitBy.isBlank() || "current-user".equals(submitBy)) {
            User op = currentUser();
            if (op != null && op.getUsername() != null) {
                submitBy = op.getUsername();
            }
        }
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
        var pageData = vehicleService.pageSearch(keyword, page, size);
        // 惰性恢复：保养已到期但状态仍为"维修保养中(3)"的车辆恢复为可用(1)
        pageData.getRecords().forEach(v -> {
            if (Integer.valueOf(3).equals(v.getStatus())) vehicleService.syncStatus(v.getId());
        });
        return Result.ok(pageData);
    }

    // ---------- 车辆维修保养（ISSUE-036） ----------
    @PostMapping("/vehicles/{id}/maintenances")
    public Result<EmsVehicleMaintenance> createMaintenance(@PathVariable Long id, @RequestBody EmsVehicleMaintenance m) {
        m.setVehicleId(id);
        return Result.ok(vehicleService.createMaintenance(m));
    }

    @GetMapping("/vehicles/{id}/maintenances")
    public Result<List<EmsVehicleMaintenance>> listMaintenances(@PathVariable Long id) {
        return Result.ok(vehicleService.listMaintenances(id));
    }

    @DeleteMapping("/vehicles/maintenances/{mid}")
    public Result<Void> deleteMaintenance(@PathVariable Long mid) {
        vehicleService.deleteMaintenance(mid);
        return Result.ok();
    }

    /** 车辆详情：基本信息 + 派单记录 + 维修保养记录（ISSUE-036） */
    @GetMapping("/vehicles/{id}/detail")
    public Result<Map<String, Object>> vehicleDetail(@PathVariable Long id) {
        return Result.ok(dispatchService.getVehicleDetail(id));
    }

    // ---------- 采样任务（5.1+5.2） ----------
    @PostMapping("/sampling-order/gen")
    public Result<Integer> genOrders(@RequestParam Long entrustId) {
        EmsEntrust e = entrustService.getById(entrustId);
        if (e == null) {
            return Result.fail(400, "委托不存在");
        }
        return Result.ok(samplingOrderService.genFromEntrust(e, currentUser()));
    }

    /** 按采集频率再次派单：同一委托单生成下一张待派单订单（委托须已确认且已有订单） */
    @PostMapping("/sampling-order/redispatch")
    public Result<EmsSamplingOrder> redispatch(@RequestParam Long entrustId) {
        try {
            return Result.ok(samplingOrderService.redispatch(entrustId, currentUser()));
        } catch (IllegalStateException ex) {
            return Result.fail(400, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    @GetMapping("/sampling-orders")
    public Result<List<EmsSamplingOrder>> listSamplingOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long entrustId) {
        return Result.ok(samplingOrderService.listByStatus(status, entrustId));
    }

    /** 采样调度看板聚合：补充点位名称、派单计划区间、派单负责人姓名，支持按订单号/负责人/状态筛选 */
    @GetMapping("/sampling-orders/dispatch-list")
    public Result<List<Map<String, Object>>> listDispatchBoard(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String leadName,
            @RequestParam(required = false) String status) {
        return Result.ok(samplingOrderService.listDispatchBoard(orderNo, leadName, status));
    }

    /** 删除采样任务（级联删除关联派单，ISSUE-037） */
    @DeleteMapping("/sampling-orders/{id}")
    public Result<Void> deleteSamplingOrder(@PathVariable Long id) {
        samplingOrderService.deleteOrder(id);
        return Result.ok();
    }

    /**
     * 采样任务完成确认：仅「已派单」可完成。负责人录入实际完成时间与完成描述（富文本），
     * 状态流转为「已完成」并记录操作历史。
     */
    @PostMapping("/sampling-orders/{id}/complete")
    public Result<com.flow.engine.entity.EmsSamplingOrder> completeSamplingOrder(
            @PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        java.time.LocalDateTime actualFinishTime = null;
        Object t = body.get("actualFinishTime");
        if (t != null && !String.valueOf(t).isBlank()) {
            actualFinishTime = parseLocalDateTime(String.valueOf(t));
        }
        String finishDesc = body.get("finishDesc") == null ? null : String.valueOf(body.get("finishDesc"));
        return Result.ok(samplingOrderService.complete(id, actualFinishTime, finishDesc, currentUser()));
    }

    /** 采样任务操作历史（完成等处置轨迹，倒序） */
    @GetMapping("/sampling-orders/{id}/history")
    public Result<List<com.flow.engine.entity.EmsSamplingOrderHistory>> samplingOrderHistory(@PathVariable Long id) {
        return Result.ok(samplingOrderService.listHistory(id));
    }

    /** 批量派单：同一组派单信息依次派发到多个订单（复用冲突/资质校验），返回成功与失败明细 */
    @PostMapping("/sampling-orders/batch-dispatch")
    public Result<Map<String, Object>> batchDispatch(
            @RequestParam List<Long> orderIds,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam Long leadId,
            @RequestParam(required = false) List<Long> empIds,
            @RequestParam(required = false) List<Long> instrumentIds,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime planStart,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime planEnd,
            @RequestParam(required = false) String note) {
        Map<String, Object> result = samplingOrderService.batchDispatch(
                orderIds, vehicleId, leadId, empIds, instrumentIds, planStart, planEnd, note, dispatchService);
        List<Map<String, Object>> successList = (List<Map<String, Object>>) result.get("successIds");
        List<Map<String, Object>> failList = (List<Map<String, Object>>) result.get("failList");
        // 存在阻断（部分订单无法派单）时返回异常状态值，前端据此保留表单并展示阻断明细
        if (failList != null && !failList.isEmpty()) {
            String detail = failList.stream()
                    .map(m -> "订单" + m.get("orderId") + "：" + m.get("reason"))
                    .collect(java.util.stream.Collectors.joining("\n"));
            java.util.Map<String, Object> block = new java.util.LinkedHashMap<>();
            block.put("failList", failList);
            block.put("successCount", successList == null ? 0 : successList.size());
            return Result.fail(ErrorCode.BUSINESS_ERROR.getCode(),
                    "派单被阻断：\n" + detail, block);
        }
        return Result.ok(result);
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

    /** 编辑派单信息：负责人/组员/车辆/设备/计划区间/备注（复用资质闸门与冲突校验），变更明细记入操作历史 */
    @PutMapping("/dispatch/{id}")
    public Result<EmsDispatch> updateDispatch(@PathVariable Long id,
                                              @RequestParam(required = false) Long vehicleId,
                                              @RequestParam(required = false) Long leadId,
                                              @RequestParam(required = false) List<Long> empIds,
                                              @RequestParam(required = false) List<Long> instrumentIds,
                                              @RequestParam(required = false) String planStart,
                                              @RequestParam(required = false) String planEnd,
                                              @RequestParam(required = false) String note) {
        java.time.LocalDateTime ps = planStart == null ? null : parseLocalDateTime(planStart);
        java.time.LocalDateTime pe = planEnd == null ? null : parseLocalDateTime(planEnd);
        return Result.ok(dispatchService.updateDispatch(id, vehicleId, leadId, empIds, instrumentIds, ps, pe, note, currentUser()));
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

    /**
     * 清空全部派单记录（派单主表 + 设备关联表 + 人员关联表）。
     * 仅清除派单数据，不影响订单/车辆/人员/设备等基础数据。谨慎调用。
     */
    @PostMapping("/dispatch/clear")
    public Result<Integer> clearAllDispatch() {
        int count = dispatchService.clearAll();
        return Result.ok(count);
    }

    /**
     * 清空全部采样派单数据（采样记录 + 采样任务 + 采样照片）。
     * 仅清理采样业务线，不影响委托单/监测点位/样品/人员/设备等基础数据。谨慎调用。
     */
    @PostMapping("/sampling/clear")
    public Result<Integer> clearAllSampling() {
        int count = samplingService.clearAll();
        return Result.ok(count);
    }

    /**
     * 车辆使用日历（ISSUE-035）：查询车辆在某时间范围内的占用区间。
     * 不传 start/end 时返回全部历史占用；传 vehicleId 仅查单车。
     */
    @GetMapping("/dispatch/vehicle-usage")
    public Result<List<Map<String, Object>>> vehicleUsage(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Long vehicleId) {
        java.time.LocalDateTime s = start == null ? null : parseLocalDateTime(start);
        java.time.LocalDateTime e = end == null ? null : parseLocalDateTime(end);
        return Result.ok(dispatchService.getVehicleUsage(s, e, vehicleId));
    }

    /**
     * 设备使用日历：查询仪器在某时间范围内的占用区间（派单 + 校准）。
     * 不传 start/end 时返回全部历史占用；传 instrumentId 仅查单台设备。
     */
    @GetMapping("/dispatch/instrument-usage")
    public Result<List<Map<String, Object>>> instrumentUsage(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Long instrumentId) {
        java.time.LocalDateTime s = start == null ? null : parseLocalDateTime(start);
        java.time.LocalDateTime e = end == null ? null : parseLocalDateTime(end);
        return Result.ok(dispatchService.getInstrumentUsage(s, e, instrumentId));
    }

    /**
     * 派单可用车辆（ISSUE-035）：给定检车时间区间，返回未被占用的车辆 id 列表。
     * 若时间区间为空，返回全部车辆。
     */
    @GetMapping("/dispatch/available-vehicles")
    public Result<List<Long>> availableVehicles(
            @RequestParam(required = false) String planStart,
            @RequestParam(required = false) String planEnd) {
        java.time.LocalDateTime ps = planStart == null ? null : parseLocalDateTime(planStart);
        java.time.LocalDateTime pe = planEnd == null ? null : parseLocalDateTime(planEnd);
        return Result.ok(dispatchService.getAvailableVehicles(ps, pe));
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

    // ---------- 采样参数配置（TRD 5.1） ----------
    /** 列表 + 条件检索：检测类别、检测项目关键字 */
    @GetMapping("/sample-param-config")
    public Result<List<Map<String, Object>>> listSampleParamConfigs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return Result.ok(sampleParamConfigService.search(type, keyword));
    }

    /** 详情（含结构化参数明细） */
    @GetMapping("/sample-param-config/{id}")
    public Result<Map<String, Object>> getSampleParamConfig(@PathVariable Long id) {
        try {
            return Result.ok(sampleParamConfigService.detail(id));
        } catch (IllegalArgumentException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }

    /** 新建 / 更新（级联保存结构化参数明细） */
    @PostMapping("/sample-param-config")
    public Result<Map<String, Object>> saveSampleParamConfig(@RequestBody SampleParamSaveReq req) {
        com.flow.engine.entity.EmsSampleParamConfig config = new com.flow.engine.entity.EmsSampleParamConfig();
        config.setId(req.getId());
        config.setType(req.getType());
        config.setItem(req.getItem());
        config.setStandard(req.getStandard());
        config.setLimitValue(req.getLimit());
        config.setRemark(req.getRemark());
        // 明细字段映射：前端 required 为 Boolean，实体 required 为 Integer(1/0)
        List<com.flow.engine.entity.EmsSampleParamItem> items = (req.getSampleParams() == null)
                ? new java.util.ArrayList<>()
                : req.getSampleParams().stream().map(it -> {
                    com.flow.engine.entity.EmsSampleParamItem e = new com.flow.engine.entity.EmsSampleParamItem();
                    e.setCode(it.getCode());
                    e.setName(it.getName());
                    e.setParamType(it.getParamType());
                    e.setUnit(it.getUnit());
                    e.setRequired(Boolean.TRUE.equals(it.getRequired()) ? 1 : 0);
                    e.setEnumText(it.getEnumText());
                    e.setTip(it.getTip());
                    return e;
                }).collect(java.util.stream.Collectors.toList());
        try {
            return Result.ok(sampleParamConfigService.saveConfig(config, items));
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    /** 删除单条配置（级联删除明细） */
    @DeleteMapping("/sample-param-config/{id}")
    public Result<Void> deleteSampleParamConfig(@PathVariable Long id) {
        sampleParamConfigService.removeConfig(id);
        return Result.ok();
    }

    /** 批量删除配置 */
    @PostMapping("/sample-param-config/batch-delete")
    public Result<Void> batchDeleteSampleParamConfig(@RequestBody List<Long> ids) {
        sampleParamConfigService.removeBatch(ids);
        return Result.ok();
    }

    /**
     * 从统一认证上下文(RequestContext)获取当前用户。
     * 用户身份由 {@link com.flow.engine.common.AuthContextFilter} 在请求早期解析并写入，
     * 业务层无需感知具体认证方式（会话 Token / API Token）。
     */
    private User currentUser() {
        String userId = com.flow.engine.common.RequestContext.current().getUserId();
        if (userId == null) {
            return null;
        }
        try {
            return userService.getUser(Long.valueOf(userId));
        } catch (Exception e) {
            return null;
        }
    }
}
