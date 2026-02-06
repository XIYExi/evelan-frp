package net.evelan.frp.bootstrap.annotation.lifecycle;


import java.lang.annotation.*;

/**
 * 仿 SpringBoot 中 @Autowired 注解，用于载入容器
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EImport {
    String value() default "";

    boolean isRequired() default true;
}
