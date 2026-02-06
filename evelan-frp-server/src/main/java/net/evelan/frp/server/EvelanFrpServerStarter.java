package net.evelan.frp.server;

import net.evelan.frp.bootstrap.core.deprecated.ServerEvelanApplication;
import net.evelan.frp.bootstrap.annotation.EvelanBootApplication;
import net.evelan.frp.bootstrap.core.deprecated.ServerApplicationContext;

@EvelanBootApplication
public class EvelanFrpServerStarter {
    public static void main(String... args) {
        ServerApplicationContext context = ServerEvelanApplication.run(EvelanFrpServerStarter.class, args);
        TestController controller = (TestController) context.getBean(TestController.class);
        controller.test();
    }
}
