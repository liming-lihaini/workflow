package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 环境监测 - 客户档案（TRD 5.1）
 */
@Data
@TableName("t_customer")
public class EmsCustomer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String custNo;
    private String custName;
    private String creditCode;
    private String contact;
    private String tel;
    private String invoiceTitle;
    private String taxNo;
    private Long qualFileId;
    private String city;      // 所在城市
    private String address;   // 办公地址
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
