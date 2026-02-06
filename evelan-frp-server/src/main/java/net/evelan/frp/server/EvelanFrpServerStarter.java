package net.evelan.frp.server;

import net.evelan.frp.bootstrap.EvelanApplication;
import net.evelan.frp.bootstrap.annotation.EvelanBootApplication;
import net.evelan.frp.bootstrap.core.EvelanApplicationContext;

@EvelanBootApplication
public class EvelanFrpServerStarter {
    public static void main(String... args) {
        EvelanApplicationContext context = EvelanApplication.run(EvelanFrpServerStarter.class, args);
        TestController controller = (TestController) context.getBean(TestController.class);
        controller.test();
    }
}
