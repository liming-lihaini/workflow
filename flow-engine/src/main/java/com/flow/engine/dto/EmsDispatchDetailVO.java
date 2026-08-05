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
    }

    @Data
    public static class InstrumentInfo {
        private Long id;
        private String code;
        private String name;
        private String model;
        private String calibDue;
    }
}
