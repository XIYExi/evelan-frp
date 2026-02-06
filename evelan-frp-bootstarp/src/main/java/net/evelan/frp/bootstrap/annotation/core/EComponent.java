package net.evelan.frp.bootstrap.annotation.core;


import java.lang.annotation.*;

/**
 * 仿 SpringBoot 中 @Component 注解，用于标识一个类为 Bean
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface EComponent {
    public String value() default "";
}
