package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("t_report")
public class EmsReport {
    private Long id;
    private String reportNo;
    private String title;
    private Long tplId;
    private String tplType;
    private String client;
    private String period;
    private String taskIds;       // JSON数组
    private Integer itemCount;
    private Integer exceedCount;
    private String status;        // 待审核/已发布/已退回
    private String antiFakeCode;  // 防伪码
    private String generator;     // 生成人
    private String publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getTplId() { return tplId; }
    public void setTplId(Long tplId) { this.tplId = tplId; }
    public String getTplType() { return tplType; }
    public void setTplType(String tplType) { this.tplType = tplType; }
    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getTaskIds() { return taskIds; }
    public void setTaskIds(String taskIds) { this.taskIds = taskIds; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getExceedCount() { return exceedCount; }
    public void setExceedCount(Integer exceedCount) { this.exceedCount = exceedCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAntiFakeCode() { return antiFakeCode; }
    public void setAntiFakeCode(String antiFakeCode) { this.antiFakeCode = antiFakeCode; }
    public String getGenerator() { return generator; }
    public void setGenerator(String generator) { this.generator = generator; }
    public String getPublishTime() { return publishTime; }
    public void setPublishTime(String publishTime) { this.publishTime = publishTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
