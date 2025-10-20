package com.divination.liuyao.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AIDoc {
    String value() default ""; // 类的描述
}

