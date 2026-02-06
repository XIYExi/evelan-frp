package net.evelan.frp.bootstrap.annotation.bean;

import java.lang.annotation.*;


/**
 * 仿 Spring 的 @Bean 注解，用于注册 Bean 对象
 */
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface EBean {
    public String value() default "";

    String initMethod() default "";

    String destroyMethod() default "";
}
