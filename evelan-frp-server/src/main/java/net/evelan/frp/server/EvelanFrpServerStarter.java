package net.evelan.frp.server;

import net.evelan.frp.bootstrap.core.AssembleApplicationContext;

public class EvelanFrpServerStarter {
    public static void main(String... args) {
        AssembleApplicationContext context = new AssembleApplicationContext("application.yaml");
        TestController controller = (TestController) context.getBean(TestController.class);
        controller.test();
    }
}
