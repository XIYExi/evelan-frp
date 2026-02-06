package net.evelan.frp.server;

import net.evelan.frp.bootstrap.annotation.core.EBean;
import net.evelan.frp.bootstrap.annotation.core.EConfiguration;

@EConfiguration
public class TestConfiguration {
    @EBean("domainFromConfig")
    public Domain domain() {
        return new Domain();
    }
}

