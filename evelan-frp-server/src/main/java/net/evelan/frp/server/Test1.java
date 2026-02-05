package net.evelan.frp.server;

import net.evelan.frp.bootstrap.annotation.EBean;
import net.evelan.frp.bootstrap.annotation.EComponent;

@EComponent("T1")
public class Test1 implements Test{

    @EBean("domain1")
    public Domain domain1() {
        return new Domain();
    }

    @EBean("domain2")
    public Domain domain2() {
        return new Domain();
    }

}
