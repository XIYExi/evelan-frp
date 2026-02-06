package net.evelan.frp.server;

import net.evelan.frp.bootstrap.annotation.bean.EComponent;
import net.evelan.frp.bootstrap.annotation.lifecycle.EImport;

@EComponent
public class TestService11 {
    private final Domain testService;

    public TestService11(@EImport("domainFromConfig") Domain myDomain) {
        testService = myDomain;
    }

    public void test() {
        System.out.println(testService.getClass());
    }

}
