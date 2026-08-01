package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 环境监测 - 派单人员关联（TRD 5.2 资源分配）
 */
@Data
@TableName("t_dispatch_member")
public class EmsDispatchMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dispatchId;
    private Long empId;         // 人员ID（关联后台用户 sys_user）
    private String role;        // LEAD-负责 / MEMBER-组员
}
