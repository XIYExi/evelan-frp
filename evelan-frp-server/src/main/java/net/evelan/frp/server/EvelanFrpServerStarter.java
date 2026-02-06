package net.evelan.frp.server;

import net.evelan.frp.bootstrap.core.context.AnnotationConfigApplicationContext;
import net.evelan.frp.bootstrap.core.deprecated.ServerEvelanApplication;
import net.evelan.frp.bootstrap.annotation.EvelanBootApplication;
import net.evelan.frp.bootstrap.core.deprecated.ServerApplicationContext;
import net.evelan.frp.bootstrap.core.solver.PropertyResolver;

import java.util.Properties;



public class EvelanFrpServerStarter {
    public static void main(String... args) {
//        ServerApplicationContext context = ServerEvelanApplication.run(EvelanFrpServerStarter.class, args);
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EvelanFrpServerStarter.class, new PropertyResolver(new Properties()));
        TestController controller = context.getBean(TestController.class);
        controller.test();
        System.out.println("\n==========\n");
        TestService11 bean = context.getBean(TestService11.class);
        bean.test();
    }
}
