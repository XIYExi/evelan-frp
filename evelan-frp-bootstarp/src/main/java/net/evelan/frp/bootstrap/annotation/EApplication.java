package net.evelan.frp.bootstrap.annotation;

import java.lang.annotation.*;

/**
 * 启动类注解，标识这是一个Evelan Boot应用
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EApplication {
}
