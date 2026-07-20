package com.flow.engine.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解（ISSUE-014）
 * <p>
 * 标注在Controller方法上，自动记录操作日志（含前后数据）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {
    
    /**
     * 模块名称
     */
    String module() default "";
    
    /**
     * 操作类型（如：创建、更新、删除）
     */
    String operation() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;
    
    /**
     * 是否记录返回结果
     */
    boolean recordResult() default false;
    
    /**
     * 是否记录修改前数据（需要配合@BeforeData使用）
     */
    boolean recordBeforeData() default false;
    
    /**
     * 是否记录修改后数据
     */
    boolean recordAfterData() default false;
}
