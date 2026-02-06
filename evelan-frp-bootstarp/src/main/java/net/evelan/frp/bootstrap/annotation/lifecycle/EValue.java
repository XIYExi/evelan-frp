package net.evelan.frp.bootstrap.annotation.lifecycle;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
public @interface EValue {
    String value();
}
