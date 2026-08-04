package com.flow.engine.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.DictType;
import com.flow.engine.mapper.DictItemMapper;
import com.flow.engine.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 系统内置字典数据初始化（ISSUE-015）
 * <p>
 * 应用启动时自动初始化系统内置字典数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(10)
public class DictDataInitializer implements CommandLineRunner {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    @Override
    public void run(String... args) {
        log.info("[DictDataInitializer] 开始初始化系统内置字典数据...");
        
        initDictTypes();
        initDictItems();
        
        log.info("[DictDataInitializer] 系统内置字典数据初始化完成");
    }

    private void initDictTypes() {
        // 1. 流程状态
        createDictTypeIfNotExists("流程状态", "process_status", 1, "流程实例的运行状态");
        
        // 2. 节点类型
        createDictTypeIfNotExists("节点类型", "node_type", 1, "流程定义中的节点类型");
        
        // 3. 任务状态
        createDictTypeIfNotExists("任务状态", "task_status", 1, "任务的处理状态");
        
        // 4. 会签模式
        createDictTypeIfNotExists("会签模式", "counter_sign_mode", 1, "会签节点的投票模式");
        
        // 5. 加签类型
        createDictTypeIfNotExists("加签类型", "sign_type", 1, "加签操作的类型");
        
        // 6. 数据权限范围
        createDictTypeIfNotExists("数据权限范围", "data_scope", 1, "角色的数据权限范围");
        
        // 7. 密级
        createDictTypeIfNotExists("密级", "security_level", 1, "用户和数据的密级");
        
        // 8. 部门类型
        createDictTypeIfNotExists("部门类型", "dept_type", 1, "部门的分类");
        
        // 9. 字典类型分类
        createDictTypeIfNotExists("字典类型分类", "dict_type_category", 1, "字典类型的分类：1-系统内置，2-业务自定义");
        
        // 10. 流程类型
        createDictTypeIfNotExists("流程类型", "process_type", 1, "流程的分类");
        
        // 11. 表单分类
        createDictTypeIfNotExists("表单分类", "form_category", 1, "表单的分类");

        // ===== 环境监测管理（ISSUE-022）业务字典 =====
        // 12. 监测类别（PRD 1.3 业务范围）
        createDictTypeIfNotExists("监测类别", "moni_monitor_type", 2, "环境监测业务监测类别枚举（地表水/废水/废气/土壤等）");
        // 13. 委托分类（TRD 5.1 t_entrust.type）
        createDictTypeIfNotExists("委托分类", "moni_entrust_type", 2, "委托单监测分类：常规/监督/应急/信访");
        // 14. 委托状态（TRD 5.1 t_entrust.status）
        createDictTypeIfNotExists("委托状态", "moni_entrust_status", 2, "委托单状态机：草稿/待技术确认/已确认/已退回");
        // 15. 点位类型（TRD 5.1 t_monitor_point.point_type）
        createDictTypeIfNotExists("点位类型", "moni_point_type", 2, "监测点位类型枚举");
        // 16. 样品类型（TRD 5.4 t_sample.type）
        createDictTypeIfNotExists("样品类型", "moni_sample_type", 2, "样品分样类型：原样/检测样/平行样/留样/质控样");
        // 17. 样品状态（TRD 5.4 t_sample.status）
        createDictTypeIfNotExists("样品状态", "moni_sample_status", 2, "样品状态机：待检/检测中/待复测/已完成/过期作废");
        // 18. 质控类型（TRD 5.3/5.6 qc_type）
        createDictTypeIfNotExists("质控类型", "moni_qc_type", 2, "现场/实验室质控类型枚举");
        // 19. 预警类型（TRD 4.3 t_alert.type）
        createDictTypeIfNotExists("预警类型", "moni_alert_type", 2, "预警类型：校准/标物/试剂/留样/超标");
        // 20. 车辆状态（TRD 4.3 t_vehicle.status）
        createDictTypeIfNotExists("车辆状态", "moni_vehicle_status", 2, "采样车辆状态：可用/占用/维修");
        // 21. 委托来源（TRD 5.1 t_entrust.source）
        createDictTypeIfNotExists("委托来源", "moni_entrust_source", 2, "委托单来源渠道：电话/网站/上门/代理");
        // 22. 仪器设备状态（TRD 5.5 t_instrument.status 全生命周期）
        createDictTypeIfNotExists("仪器设备状态", "moni_instrument_status", 2, "仪器设备台账状态：在用/临期/停用/维修/报废");
        // 23. 监测因子（检测项目，TRD 5.1 t_monitor_point.factors）
        createDictTypeIfNotExists("监测因子", "moni_monitor_factor", 2, "监测点位检测项目/监测因子枚举");
        // 24. 执行标准（TRD 5.1 t_monitor_point.standard_code/standard_name）
        createDictTypeIfNotExists("执行标准", "moni_exec_standard", 2, "监测执行标准：标准编号 + 全称");
        // 25. 监测频次（TRD 5.1 t_monitor_point.freq）
        createDictTypeIfNotExists("监测频次", "moni_monitor_freq", 2, "监测频次/监测天数枚举");
        // 26. 车辆维修保养操作类型（ISSUE-036 t_vehicle_maintenance.maint_type）
        createDictTypeIfNotExists("车辆维修保养类型", "moni_vehicle_maint_type", 2, "维修保养操作类型：保养/维修/年检/其他");
        // 27. 危化品类别（TRD 5.5 t_hazardous_ledger.category）
        createDictTypeIfNotExists("危化品类别", "moni_hazardous_category", 2, "危化品台账类别枚举：易燃/腐蚀/有毒/易爆");
    }

    private void initDictItems() {
        // 流程状态
        DictType processStatusType = getDictTypeByCode("process_status");
        if (processStatusType != null) {
            createDictItemIfNotExists(processStatusType.getId(), "运行中", "running", 1);
            createDictItemIfNotExists(processStatusType.getId(), "已完成", "completed", 2);
            createDictItemIfNotExists(processStatusType.getId(), "已终止", "terminated", 3);
            createDictItemIfNotExists(processStatusType.getId(), "已挂起", "suspended", 4);
        }
        
        // 节点类型
        DictType nodeTypeType = getDictTypeByCode("node_type");
        if (nodeTypeType != null) {
            createDictItemIfNotExists(nodeTypeType.getId(), "开始节点", "start", 1);
            createDictItemIfNotExists(nodeTypeType.getId(), "结束节点", "end", 2);
            createDictItemIfNotExists(nodeTypeType.getId(), "用户任务", "userTask", 3);
            createDictItemIfNotExists(nodeTypeType.getId(), "服务任务", "serviceTask", 4);
            createDictItemIfNotExists(nodeTypeType.getId(), "脚本任务", "scriptTask", 5);
            createDictItemIfNotExists(nodeTypeType.getId(), "排他网关", "exclusiveGateway", 6);
            createDictItemIfNotExists(nodeTypeType.getId(), "并行网关", "parallelGateway", 7);
            createDictItemIfNotExists(nodeTypeType.getId(), "包容网关", "inclusiveGateway", 8);
            createDictItemIfNotExists(nodeTypeType.getId(), "子流程", "subProcess", 9);
            createDictItemIfNotExists(nodeTypeType.getId(), "会签节点", "counterSign", 10);
        }
        
        // 任务状态
        DictType taskStatusType = getDictTypeByCode("task_status");
        if (taskStatusType != null) {
            createDictItemIfNotExists(taskStatusType.getId(), "待处理", "pending", 1);
            createDictItemIfNotExists(taskStatusType.getId(), "已完成", "completed", 2);
            createDictItemIfNotExists(taskStatusType.getId(), "已驳回", "rejected", 3);
            createDictItemIfNotExists(taskStatusType.getId(), "已撤回", "withdrawn", 4);
            createDictItemIfNotExists(taskStatusType.getId(), "已转办", "transferred", 5);
            createDictItemIfNotExists(taskStatusType.getId(), "已委托", "delegated", 6);
        }
        
        // 会签模式
        DictType counterSignModeType = getDictTypeByCode("counter_sign_mode");
        if (counterSignModeType != null) {
            createDictItemIfNotExists(counterSignModeType.getId(), "一票通过", "any", 1);
            createDictItemIfNotExists(counterSignModeType.getId(), "全票通过", "all", 2);
            createDictItemIfNotExists(counterSignModeType.getId(), "比例通过", "ratio", 3);
            createDictItemIfNotExists(counterSignModeType.getId(), "票数通过", "count", 4);
        }
        
        // 加签类型
        DictType signTypeType = getDictTypeByCode("sign_type");
        if (signTypeType != null) {
            createDictItemIfNotExists(signTypeType.getId(), "前加签", "before", 1);
            createDictItemIfNotExists(signTypeType.getId(), "后加签", "after", 2);
            createDictItemIfNotExists(signTypeType.getId(), "并行加签", "parallel", 3);
        }
        
        // 数据权限范围
        DictType dataScopeType = getDictTypeByCode("data_scope");
        if (dataScopeType != null) {
            createDictItemIfNotExists(dataScopeType.getId(), "全部数据", "1", 1);
            createDictItemIfNotExists(dataScopeType.getId(), "本部门数据", "2", 2);
            createDictItemIfNotExists(dataScopeType.getId(), "本部门及子部门数据", "3", 3);
            createDictItemIfNotExists(dataScopeType.getId(), "仅本人数据", "4", 4);
        }
        
        // 密级
        DictType securityLevelType = getDictTypeByCode("security_level");
        if (securityLevelType != null) {
            createDictItemIfNotExists(securityLevelType.getId(), "公开", "1", 1);
            createDictItemIfNotExists(securityLevelType.getId(), "内部", "2", 2);
            createDictItemIfNotExists(securityLevelType.getId(), "秘密", "3", 3);
            createDictItemIfNotExists(securityLevelType.getId(), "机密", "4", 4);
        }
        
        // 部门类型
        DictType deptTypeType = getDictTypeByCode("dept_type");
        if (deptTypeType != null) {
            createDictItemIfNotExists(deptTypeType.getId(), "公司", "company", 1);
            createDictItemIfNotExists(deptTypeType.getId(), "部门", "dept", 2);
            createDictItemIfNotExists(deptTypeType.getId(), "小组", "group", 3);
        }
        
        // 字典类型分类
        DictType dictTypeCategoryType = getDictTypeByCode("dict_type_category");
        if (dictTypeCategoryType != null) {
            createDictItemIfNotExists(dictTypeCategoryType.getId(), "系统内置", "1", 1);
            createDictItemIfNotExists(dictTypeCategoryType.getId(), "业务自定义", "2", 2);
        }
        
        // 流程类型
        DictType processTypeType = getDictTypeByCode("process_type");
        if (processTypeType != null) {
            createDictItemIfNotExists(processTypeType.getId(), "审批流", "approval", 1);
            createDictItemIfNotExists(processTypeType.getId(), "业务流程", "process", 2);
            createDictItemIfNotExists(processTypeType.getId(), "回调流程", "callback", 3);
        }
        
        // 表单分类
        DictType formCategoryType = getDictTypeByCode("form_category");
        if (formCategoryType != null) {
            createDictItemIfNotExists(formCategoryType.getId(), "审批表单", "approval", 1);
            createDictItemIfNotExists(formCategoryType.getId(), "申请表单", "apply", 2);
            createDictItemIfNotExists(formCategoryType.getId(), "报销表单", "reimbursement", 3);
            createDictItemIfNotExists(formCategoryType.getId(), "考勤表单", "attendance", 4);
            createDictItemIfNotExists(formCategoryType.getId(), "其他", "other", 5);
        }

        // ===== 环境监测管理（ISSUE-022）业务字典项 =====
        // 监测类别（PRD 1.3 业务范围）
        DictType monitorType = getDictTypeByCode("moni_monitor_type");
        if (monitorType != null) {
            createDictItemIfNotExists(monitorType.getId(), "地表水", "surface_water", 1);
            createDictItemIfNotExists(monitorType.getId(), "地下水", "groundwater", 2);
            createDictItemIfNotExists(monitorType.getId(), "生活污水", "domestic_sewage", 3);
            createDictItemIfNotExists(monitorType.getId(), "工业废水", "industrial_wastewater", 4);
            createDictItemIfNotExists(monitorType.getId(), "有组织废气", "organized_exhaust", 5);
            createDictItemIfNotExists(monitorType.getId(), "无组织废气", "unorganized_exhaust", 6);
            createDictItemIfNotExists(monitorType.getId(), "土壤", "soil", 7);
            createDictItemIfNotExists(monitorType.getId(), "沉积物", "sediment", 8);
            createDictItemIfNotExists(monitorType.getId(), "固废", "solid_waste", 9);
            createDictItemIfNotExists(monitorType.getId(), "噪声", "noise", 10);
            createDictItemIfNotExists(monitorType.getId(), "振动", "vibration", 11);
            createDictItemIfNotExists(monitorType.getId(), "底泥", "bottom_sediment", 12);
        }

        // 委托分类（TRD 5.1 t_entrust.type）
        DictType entrustType = getDictTypeByCode("moni_entrust_type");
        if (entrustType != null) {
            createDictItemIfNotExists(entrustType.getId(), "常规监测", "routine", 1);
            createDictItemIfNotExists(entrustType.getId(), "监督监测", "supervision", 2);
            createDictItemIfNotExists(entrustType.getId(), "应急监测", "emergency", 3);
            createDictItemIfNotExists(entrustType.getId(), "信访专项", "petition", 4);
        }

        // 委托状态（TRD 5.1 t_entrust.status）
        DictType entrustStatus = getDictTypeByCode("moni_entrust_status");
        if (entrustStatus != null) {
            createDictItemIfNotExists(entrustStatus.getId(), "草稿", "draft", 1);
            createDictItemIfNotExists(entrustStatus.getId(), "待技术确认", "pending_review", 2);
            createDictItemIfNotExists(entrustStatus.getId(), "已确认", "confirmed", 3);
            createDictItemIfNotExists(entrustStatus.getId(), "已退回", "returned", 4);
        }

        // 点位类型（TRD 5.1 t_monitor_point.point_type）
        DictType pointType = getDictTypeByCode("moni_point_type");
        if (pointType != null) {
            createDictItemIfNotExists(pointType.getId(), "排污口", "discharge_outlet", 1);
            createDictItemIfNotExists(pointType.getId(), "地表水断面", "surface_section", 2);
            createDictItemIfNotExists(pointType.getId(), "土壤点位", "soil_point", 3);
            createDictItemIfNotExists(pointType.getId(), "地下水井", "groundwater_well", 4);
            createDictItemIfNotExists(pointType.getId(), "噪声点位", "noise_point", 5);
            createDictItemIfNotExists(pointType.getId(), "废气排气筒", "exhaust_stack", 6);
        }

        // 样品类型（TRD 5.4 t_sample.type）
        DictType sampleType = getDictTypeByCode("moni_sample_type");
        if (sampleType != null) {
            createDictItemIfNotExists(sampleType.getId(), "原样", "original", 1);
            createDictItemIfNotExists(sampleType.getId(), "检测样", "detect", 2);
            createDictItemIfNotExists(sampleType.getId(), "平行样", "parallel", 3);
            createDictItemIfNotExists(sampleType.getId(), "留样", "retain", 4);
            createDictItemIfNotExists(sampleType.getId(), "质控样", "qc", 5);
        }

        // 样品状态（TRD 5.4 t_sample.status）
        DictType sampleStatus = getDictTypeByCode("moni_sample_status");
        if (sampleStatus != null) {
            createDictItemIfNotExists(sampleStatus.getId(), "待检", "pending", 1);
            createDictItemIfNotExists(sampleStatus.getId(), "检测中", "testing", 2);
            createDictItemIfNotExists(sampleStatus.getId(), "待复测", "retest", 3);
            createDictItemIfNotExists(sampleStatus.getId(), "已完成", "completed", 4);
            createDictItemIfNotExists(sampleStatus.getId(), "过期作废", "expired", 5);
        }

        // 质控类型（TRD 5.3 现场空白/平行/运输空白；5.6 实验室质控）
        DictType qcType = getDictTypeByCode("moni_qc_type");
        if (qcType != null) {
            createDictItemIfNotExists(qcType.getId(), "现场空白", "field_blank", 1);
            createDictItemIfNotExists(qcType.getId(), "平行样", "parallel", 2);
            createDictItemIfNotExists(qcType.getId(), "运输空白", "transport_blank", 3);
            createDictItemIfNotExists(qcType.getId(), "加标回收", "spiked_recovery", 4);
            createDictItemIfNotExists(qcType.getId(), "空白对照", "blank_control", 5);
        }

        // 预警类型（TRD 4.3 t_alert.type）
        DictType alertType = getDictTypeByCode("moni_alert_type");
        if (alertType != null) {
            createDictItemIfNotExists(alertType.getId(), "校准", "calibration", 1);
            createDictItemIfNotExists(alertType.getId(), "标物", "reference_material", 2);
            createDictItemIfNotExists(alertType.getId(), "试剂", "reagent", 3);
            createDictItemIfNotExists(alertType.getId(), "留样", "retain", 4);
            createDictItemIfNotExists(alertType.getId(), "超标", "over_standard", 5);
        }

        // 车辆状态（TRD 4.3 t_vehicle.status）
        DictType vehicleStatus = getDictTypeByCode("moni_vehicle_status");
        if (vehicleStatus != null) {
            createDictItemIfNotExists(vehicleStatus.getId(), "可用", "available", 1);
            createDictItemIfNotExists(vehicleStatus.getId(), "占用", "occupied", 2);
            createDictItemIfNotExists(vehicleStatus.getId(), "维修保养中", "repair", 3);
        }

        // 车辆维修保养操作类型（ISSUE-036 t_vehicle_maintenance.maint_type）
        DictType maintType = getDictTypeByCode("moni_vehicle_maint_type");
        if (maintType != null) {
            createDictItemIfNotExists(maintType.getId(), "保养", "maintain", 1);
            createDictItemIfNotExists(maintType.getId(), "维修", "repair", 2);
            createDictItemIfNotExists(maintType.getId(), "年检", "annual", 3);
            createDictItemIfNotExists(maintType.getId(), "其他", "other", 4);
        }

        // 委托来源（TRD 5.1 t_entrust.source）
        DictType entrustSource = getDictTypeByCode("moni_entrust_source");
        if (entrustSource != null) {
            createDictItemIfNotExists(entrustSource.getId(), "电话委托", "phone", 1);
            createDictItemIfNotExists(entrustSource.getId(), "网站委托", "web", 2);
            createDictItemIfNotExists(entrustSource.getId(), "上门委托", "visit", 3);
            createDictItemIfNotExists(entrustSource.getId(), "代理委托", "agent", 4);
        }

        // 仪器设备状态（TRD 5.5 t_instrument.status）
        DictType instrumentStatus = getDictTypeByCode("moni_instrument_status");
        if (instrumentStatus != null) {
            createDictItemIfNotExists(instrumentStatus.getId(), "在用", "在用", 1);
            createDictItemIfNotExists(instrumentStatus.getId(), "临期", "临期", 2);
            createDictItemIfNotExists(instrumentStatus.getId(), "停用", "停用", 3);
            createDictItemIfNotExists(instrumentStatus.getId(), "维修", "维修", 4);
            createDictItemIfNotExists(instrumentStatus.getId(), "报废", "报废", 5);
        }

        // 监测因子（检测项目，TRD 5.1 t_monitor_point.factors）
        DictType factor = getDictTypeByCode("moni_monitor_factor");
        if (factor != null) {
            createDictItemIfNotExists(factor.getId(), "pH", "pH", 1);
            createDictItemIfNotExists(factor.getId(), "化学需氧量(COD)", "COD", 2);
            createDictItemIfNotExists(factor.getId(), "氨氮", "NH3N", 3);
            createDictItemIfNotExists(factor.getId(), "总磷", "TP", 4);
            createDictItemIfNotExists(factor.getId(), "总氮", "TN", 5);
            createDictItemIfNotExists(factor.getId(), "悬浮物(SS)", "SS", 6);
            createDictItemIfNotExists(factor.getId(), "溶解氧(DO)", "DO", 7);
            createDictItemIfNotExists(factor.getId(), "五日生化需氧量(BOD5)", "BOD5", 8);
            createDictItemIfNotExists(factor.getId(), "石油类", "oil", 9);
            createDictItemIfNotExists(factor.getId(), "挥发酚", "phenol", 10);
            createDictItemIfNotExists(factor.getId(), "总铅", "Pb", 11);
            createDictItemIfNotExists(factor.getId(), "总镉", "Cd", 12);
            createDictItemIfNotExists(factor.getId(), "总砷", "As", 13);
            createDictItemIfNotExists(factor.getId(), "六价铬", "Cr6", 14);
            createDictItemIfNotExists(factor.getId(), "二氧化硫(SO2)", "SO2", 15);
            createDictItemIfNotExists(factor.getId(), "氮氧化物(NOx)", "NOx", 16);
            createDictItemIfNotExists(factor.getId(), "颗粒物", "PM", 17);
            createDictItemIfNotExists(factor.getId(), "噪声(Leq)", "noise", 18);
            createDictItemIfNotExists(factor.getId(), "铅", "Pb_air", 19);
            createDictItemIfNotExists(factor.getId(), "苯并[a]芘", "BaP", 20);
        }

        // 执行标准（标准编号 + 全称，TRD 5.1 t_monitor_point.standard_code/standard_name）
        DictType standard = getDictTypeByCode("moni_exec_standard");
        if (standard != null) {
            createDictItemIfNotExists(standard.getId(), "GB 3838-2002 地表水环境质量标准", "GB3838-2002", 1);
            createDictItemIfNotExists(standard.getId(), "GB 3097-1997 海水水质标准", "GB3097-1997", 2);
            createDictItemIfNotExists(standard.getId(), "GB/T 14848-2017 地下水质量标准", "GB14848-2017", 3);
            createDictItemIfNotExists(standard.getId(), "GB 15618-2018 土壤环境质量 农用地污染风险管控标准", "GB15618-2018", 4);
            createDictItemIfNotExists(standard.getId(), "GB 3095-2012 环境空气质量标准", "GB3095-2012", 5);
            createDictItemIfNotExists(standard.getId(), "GB 3096-2008 声环境质量标准", "GB3096-2008", 6);
            createDictItemIfNotExists(standard.getId(), "GB 8978-1996 污水综合排放标准", "GB8978-1996", 7);
            createDictItemIfNotExists(standard.getId(), "GB 16297-1996 大气污染物综合排放标准", "GB16297-1996", 8);
            createDictItemIfNotExists(standard.getId(), "HJ 828-2017 水质 化学需氧量的测定 重铬酸盐法", "HJ828-2017", 9);
            createDictItemIfNotExists(standard.getId(), "HJ 535-2009 水质 氨氮的测定 纳氏试剂分光光度法", "HJ535-2009", 10);
        }

        // 监测频次 / 天数（TRD 5.1 t_monitor_point.freq）
        DictType freq = getDictTypeByCode("moni_monitor_freq");
        if (freq != null) {
            createDictItemIfNotExists(freq.getId(), "每周1次", "weekly_1", 1);
            createDictItemIfNotExists(freq.getId(), "每旬1次", "ten_days_1", 2);
            createDictItemIfNotExists(freq.getId(), "每月1次", "monthly_1", 3);
            createDictItemIfNotExists(freq.getId(), "每季度1次", "quarterly_1", 4);
            createDictItemIfNotExists(freq.getId(), "每半年1次", "halfyear_1", 5);
            createDictItemIfNotExists(freq.getId(), "每年1次", "yearly_1", 6);
            createDictItemIfNotExists(freq.getId(), "连续监测3天", "cont_3d", 7);
            createDictItemIfNotExists(freq.getId(), "连续监测7天", "cont_7d", 8);
            createDictItemIfNotExists(freq.getId(), "一次性", "once", 9);
        }

        // 危化品类别（TRD 5.5 t_hazardous_ledger.category）
        DictType hazardousCategory = getDictTypeByCode("moni_hazardous_category");
        if (hazardousCategory != null) {
            createDictItemIfNotExists(hazardousCategory.getId(), "易燃", "易燃", 1);
            createDictItemIfNotExists(hazardousCategory.getId(), "腐蚀", "腐蚀", 2);
            createDictItemIfNotExists(hazardousCategory.getId(), "有毒", "有毒", 3);
            createDictItemIfNotExists(hazardousCategory.getId(), "易爆", "易爆", 4);
            createDictItemIfNotExists(hazardousCategory.getId(), "易制毒", "易制毒", 5);
            createDictItemIfNotExists(hazardousCategory.getId(), "易制爆", "易制爆", 6);
        }
    }

    private void createDictTypeIfNotExists(String dictName, String dictCode, int dictType, String description) {
        DictType existing = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<DictType>().eq(DictType::getDictCode, dictCode)
        );
        if (existing == null) {
            DictType dictTypeEntity = new DictType();
            dictTypeEntity.setDictName(dictName);
            dictTypeEntity.setDictCode(dictCode);
            dictTypeEntity.setDictType(dictType);
            dictTypeEntity.setDescription(description);
            dictTypeEntity.setStatus(1);
            dictTypeEntity.setCreateTime(LocalDateTime.now());
            dictTypeEntity.setUpdateTime(LocalDateTime.now());
            dictTypeMapper.insert(dictTypeEntity);
            log.debug("[DictDataInitializer] 创建字典类型: code={}", dictCode);
        }
    }

    private void createDictItemIfNotExists(Long dictTypeId, String itemText, String itemValue, int sortOrder) {
        DictItem existing = dictItemMapper.selectOne(
                new LambdaQueryWrapper<DictItem>()
                        .eq(DictItem::getDictTypeId, dictTypeId)
                        .eq(DictItem::getItemValue, itemValue)
        );
        if (existing == null) {
            DictItem dictItem = new DictItem();
            dictItem.setDictTypeId(dictTypeId);
            dictItem.setItemText(itemText);
            dictItem.setItemValue(itemValue);
            dictItem.setSortOrder(sortOrder);
            dictItem.setStatus(1);
            dictItem.setCreateTime(LocalDateTime.now());
            dictItem.setUpdateTime(LocalDateTime.now());
            dictItemMapper.insert(dictItem);
            log.debug("[DictDataInitializer] 创建字典项: text={}, value={}", itemText, itemValue);
        }
    }

    private DictType getDictTypeByCode(String dictCode) {
        return dictTypeMapper.selectOne(
                new LambdaQueryWrapper<DictType>().eq(DictType::getDictCode, dictCode)
        );
    }
}
