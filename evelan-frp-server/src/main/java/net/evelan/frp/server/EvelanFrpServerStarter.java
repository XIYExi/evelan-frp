package net.evelan.frp.server;

import net.evelan.frp.bootstrap.annotation.EvelanBootApplication;
import net.evelan.frp.bootstrap.core.EvelanApplication;
import net.evelan.frp.bootstrap.core.context.EvelanConfigApplicationContext;
import net.evelan.frp.bootstrap.core.solver.PropertyResolver;

import java.util.Properties;


@EvelanBootApplication
public class EvelanFrpServerStarter {
    public static void main(String... args) {
        EvelanConfigApplicationContext context = EvelanApplication.run(EvelanFrpServerStarter.class, args);
        TestController controller = context.getBean(TestController.class);
        controller.test();
        System.out.println("\n==========\n");
        TestService11 bean = context.getBean(TestService11.class);
        bean.test();
    }
}
