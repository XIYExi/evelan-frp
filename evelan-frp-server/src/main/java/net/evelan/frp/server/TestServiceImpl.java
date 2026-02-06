package net.evelan.frp.server;

import net.evelan.frp.bootstrap.annotation.EComponent;

@EComponent
public class TestServiceImpl implements TestService{
    @Override
    public void test(int a) {
        int b = 2;
        System.out.println("result: " + (a + b));
    }
}
