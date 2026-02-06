package net.evelan.frp.bootstrap.core.core;

import java.util.Objects;

public class ApplicationContextUtils {
    private static ApplicationContext applicationContext = null;

    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext(), "Application is not set.");
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext ctx) {
        ApplicationContextUtils.applicationContext = ctx;
    }
}
