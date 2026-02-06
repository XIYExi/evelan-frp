package net.evelan.frp.bootstrap.annotation.lifecycle;

import java.lang.annotation.*;

/**
 * 仿 Spring 的 @PostConstruct 注解，用于在依赖注入完成后执行初始化方法
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EPostConstruct {
}
