package net.evelan.frp.bootstrap.annotation;

import java.lang.annotation.*;

/**
 * 仿 SpringBoot 中 @Mapper 注解，用于标识一个类为 Mapper
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EMapper {
    String value() default "";
}
