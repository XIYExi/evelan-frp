package net.evelan.frp.server;

import net.evelan.frp.bootstrap.annotation.EController;
import net.evelan.frp.bootstrap.annotation.EImport;

@EController("controller")
public class TestController {

    @EImport
    private TestService testService;

    public void test() {
        System.out.println("test controller");
        testService.test(1);
        System.out.println("ioc inject...");
    }

}
