package net.evelan.frp.bootstrap.annotation.bean;

import java.lang.annotation.*;

@EComponent
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface EService {
    String value() default "";
}

