package net.evelan.frp.server;

import net.evelan.frp.bootstrap.annotation.bean.EBean;
import net.evelan.frp.bootstrap.annotation.bean.EConfiguration;

@EConfiguration
public class TestConfiguration {
    @EBean("domainFromConfig")
    public Domain domain() {
        return new Domain();
    }
}

