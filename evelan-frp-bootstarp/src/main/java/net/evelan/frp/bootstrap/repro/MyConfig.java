package net.evelan.frp.bootstrap.repro;

import net.evelan.frp.bootstrap.annotation.EBean;
import net.evelan.frp.bootstrap.annotation.EConfiguration;

@EConfiguration
public class MyConfig {
    @EBean
    public String myString() {
        return "Hello World";
    }
}
