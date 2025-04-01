package com.divination.liuyao.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口访问限流注解
 * 在需要限流的方法上添加此注解即可
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流的时间窗口，默认1分钟
     */
    int period() default 60;
    
    /**
     * 时间窗口单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 在时间窗口内允许的最大请求次数，默认10次
     */
    int maxRequests() default 10;
    
    /**
     * 限流的key前缀，默认按照类名+方法名来限流
     * 如果设置了此值，将按照此值+用户ID来限流
     */
    String key() default "";
    
    /**
     * 触发限流时的提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
} 