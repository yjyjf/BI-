package com.yjf.bi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解：结合aop,校验是否还有调用次数
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseCheck {
    //输入方法名（同步或异步），aop中进行不同的处理
    String method() default "";
}
