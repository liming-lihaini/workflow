package com.flow.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 派单详情视图对象（TRD 5.2）：聚合订单、派单主表、负责人/组员、车辆、设备。
 */
@Data
public class EmsDispatchDetailVO {
    private Long orderId;
    private String orderNo;
    private String orderStatus;
    /** 任务创建人（账号/姓名）与创建时间 */
    private String createBy;
    private String createName;
    private LocalDateTime orderCreateTime;
    private Long entrustId;
    private String entrustNo;
    private String entrustName;
    private String entrustStatus;
    private Integer pointCount;
    private Long dispatchId;
    private LocalDateTime dispatchTime;
    private LocalDateTime planStart;
    private LocalDateTime planEnd;
    private String note;
    private VehicleInfo vehicle;
    private MemberInfo lead;
    private List<MemberInfo> members;
    private List<InstrumentInfo> instruments;
    /** 任务关联样品（样品登记时回写 t_sample.order_id） */
    private List<SampleInfo> samples;

    @Data
    public static class VehicleInfo {
        private Long id;
        private String plateNo;
        private String model;
    }

    @Data
    public static class MemberInfo {
        private Long userId;
        private String realName;
        private String username;
        private String role; // LEAD-负责 / MEMBER-组员
        /** 人员资质名称列表（sys_user_qualification） */
        private List<String> qualNames;
    }

    @Data
    public static class InstrumentInfo {
        private Long id;
        private String code;
        private String name;
        private String model;
        private String calibDue;
    }

    @Data
    public static class SampleInfo {
        private Long id;
        private String barcode;    // 样品条码
        private String sampleNo;   // 样品编号
        private String name;       // 样品名称
        private String category;   // 样品类别
        private String item;       // 检测项目
        private String type;       // 样品类型
        private String status;     // 样品状态
        private String sampler;    // 采样人
        private String sampleTime; // 采样时间
        private String receiveBy;  // 收样人
        private String receiveTime;// 收样时间
    }
}
