package com.flow.engine.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 客户 Excel 导入/模板 VO（表头与中文列名对应）
 */
@Data
public class EmsCustomerExcelVO {

    @ExcelProperty("客户名称")
    private String custName;

    @ExcelProperty("统一信用代码")
    private String creditCode;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("联系电话")
    private String tel;

    @ExcelProperty("开票抬头")
    private String invoiceTitle;

    @ExcelProperty("税号")
    private String taxNo;

    @ExcelProperty("所在城市")
    private String city;

    @ExcelProperty("办公地址")
    private String address;
}
