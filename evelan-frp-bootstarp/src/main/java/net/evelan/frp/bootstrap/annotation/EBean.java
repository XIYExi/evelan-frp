package net.evelan.frp.bootstrap.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * 仿 Spring 的 @Bean 注解，用于注册 Bean 对象
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface EBean {
    public String value() default "";
}
