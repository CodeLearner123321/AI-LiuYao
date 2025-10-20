package com.divination.liuyao.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AIDocField {
    String desc() default "";  // 字段说明
    boolean required() default true; // 是否必填
}
