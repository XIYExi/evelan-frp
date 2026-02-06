package net.evelan.frp.bootstrap.annotation.core;

import java.lang.annotation.*;

/**
 * 启动类注解，标识这是一个Evelan Boot应用
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EConfiguration
@EComponent
public @interface EvelanBootApplication {
}
