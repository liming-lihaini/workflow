package com.flow.engine.dto;

import lombok.Data;

@Data
public class ReceiveReq {

    /** 收样人 */
    private String receiveBy;

    /** 收样时间 YYYY-MM-DD */
    private String receiveTime;

    /** 备注 */
    private String remark;

    /** 数量规格 */
    private String amount;

    /** 保存容器 */
    private String container;

    /** 保存条件 */
    private String preserve;

    /** 收样检查单（多选，逗号分隔数据字典值），来源 sample_receive_check */
    private String checkItems;

    /** 收样动作：receive=正常收样（默认），reject=异常拒收 */
    private String action;

    /** 是否留样 0否 1是 */
    private Integer retainFlag;

    /** 留样天数 */
    private Integer retainDays;

    /** 留样人 */
    private String retainBy;

    /** 留样日期 YYYY-MM-DD，默认今天 */
    private String retainDate;

    /** 存放位置 */
    private String retainLocation;
}
