package net.evelan.frp.bootstrap.annotation;

import net.evelan.frp.bootstrap.annotation.bean.EComponent;
import net.evelan.frp.bootstrap.annotation.bean.EConfiguration;

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
