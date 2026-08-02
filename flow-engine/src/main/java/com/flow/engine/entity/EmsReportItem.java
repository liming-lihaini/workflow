package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("t_report_item")
public class EmsReportItem {
    private Long id;
    private Long reportId;
    private Long taskId;
    private String item;
    private String sampleCode;
    private String result;
    private String unit;
    private String standardLimit;
    private String conclusion;   // 达标/超标
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getSampleCode() { return sampleCode; }
    public void setSampleCode(String sampleCode) { this.sampleCode = sampleCode; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getStandardLimit() { return standardLimit; }
    public void setStandardLimit(String standardLimit) { this.standardLimit = standardLimit; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
