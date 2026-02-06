package net.evelan.frp.bootstrap.annotation;

import java.lang.annotation.*;

@EComponent
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface EConfiguration {
    String value() default "";
}

