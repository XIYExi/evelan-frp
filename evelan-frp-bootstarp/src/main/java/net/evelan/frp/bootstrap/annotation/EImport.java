package net.evelan.frp.bootstrap.annotation;


import java.lang.annotation.*;

/**
 * 仿 SpringBoot 中 @Autowired 注解，用于载入容器
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EImport {
}
