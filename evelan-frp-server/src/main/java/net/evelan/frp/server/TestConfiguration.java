package net.evelan.frp.server;

import net.evelan.frp.bootstrap.annotation.EBean;
import net.evelan.frp.bootstrap.annotation.EConfiguration;

@EConfiguration
public class TestConfiguration {
    @EBean("domainFromConfig")
    public Domain domain() {
        return new Domain();
    }
}

