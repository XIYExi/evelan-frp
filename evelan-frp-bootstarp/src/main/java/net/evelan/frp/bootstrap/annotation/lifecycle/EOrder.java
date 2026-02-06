package net.evelan.frp.bootstrap.annotation.lifecycle;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EOrder {
    int value();
}
