package net.evelan.frp.server;

import java.lang.reflect.Proxy;

public class TestProxy {
    public static void main(String[] args) {
        final TestServiceImpl testService = new TestServiceImpl();

        TestService testServiceProxy = (TestService) Proxy.newProxyInstance(TestService.class.getClassLoader(), TestServiceImpl.class.getInterfaces(), (proxy, method, args1) -> {
            Object result = null;
            try {
                System.out.println("前置");
                result = method.invoke(testService, args1);
                System.out.println("返回通知");
            } catch (Exception e) {
                System.out.println("异常通知");
                e.printStackTrace();
            } finally {
                System.out.println("最终通知");
            }
            return result;
        });

        testServiceProxy.test(1);
    }
}
