package net.evelan.frp.bootstrap;

import net.evelan.frp.bootstrap.core.EvelanApplicationContext;

/**
 * 应用启动类
 */
public class EvelanApplication {

    /**
     * 启动应用
     * @param primarySource 主类
     * @param args 启动参数
     * @return 应用上下文
     */
    public static EvelanApplicationContext run(Class<?> primarySource, String... args) {
        long startTime = System.currentTimeMillis();
        System.out.println("Starting Evelan Application using Java " + System.getProperty("java.version"));
        
        // 创建并刷新应用上下文
        // 这里会自动推断 primarySource 所在的包作为扫描的基础包
        EvelanApplicationContext context = new EvelanApplicationContext(primarySource);
        
        long endTime = System.currentTimeMillis();
        System.out.println("Evelan Application started in " + (endTime - startTime) / 1000.0 + " seconds");
        
        return context;
    }
}
