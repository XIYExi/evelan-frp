package net.evelan.frp.bootstrap.annotation.bean;

import java.lang.annotation.*;

/**
 * 仿 SpringBoot 中 @Controller 注解，用于标识一个类为 Controller
 */
@EComponent
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface EController {
    public String value() default "";
}
