package net.evelan.frp.server;

import net.evelan.frp.bootstrap.core.AssembleApplicationContext;
import net.evelan.frp.bootstrap.repro.MyComponent;

public class EvelanFrpServerStarter {
    public static void main(String... args) {
        AssembleApplicationContext context = new AssembleApplicationContext();
        TestController controller = (TestController) context.getBean(TestController.class);
        controller.test();
        MyComponent myComponent = (MyComponent) context.getBean(MyComponent.class);
        String myString = myComponent.getMyString();
        System.out.println(myString);
    }
}
