package com.flow.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 设备详情视图对象（TRD 5.5）：基本信息 + 校准记录 + 关联采样任务。
 */
@Data
public class EmsInstrumentDetailVO {
    private Long id;
    private String code;
    private String name;
    private String model;
    private String manufacturer;
    private String purchaseDate;
    private String calibDue;
    private String lastCalibDate;
    private String certNo;
    private String status;
    private String remark;

    private List<CalibRecord> calibRecords;
    private List<SamplingTask> samplingTasks;

    @Data
    public static class CalibRecord {
        private String calibDate;
        private String calibDue;
        private String certNo;
        private String createTime;
    }

    @Data
    public static class SamplingTask {
        private Long orderId;
        private String orderNo;
        private String orderStatus;
        private String planStart;
        private String planEnd;
    }
}
