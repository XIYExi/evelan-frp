package net.evelan.frp.bootstrap.core.deprecated;

import net.evelan.frp.bootstrap.annotation.EvelanBootApplication;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用启动类
 */
@Deprecated
public class ServerEvelanApplication {

    /**
     * 记录已启动的应用主类，防止重复启动
     */
    private static final Set<Class<?>> startedApplications = ConcurrentHashMap.newKeySet();

    /**
     * 启动应用
     * @param primarySource 主类
     * @param args 启动参数
     * @return 应用上下文
     */
    public static ServerApplicationContext run(Class<?> primarySource, String... args) {
        // 检查是否有 @EvelanBootApplication 注解
        if (!primarySource.isAnnotationPresent(EvelanBootApplication.class)) {
            throw new RuntimeException("Startup failed: The primary source " + primarySource.getName() + 
                " is not annotated with @EvelanBootApplication");
        }

        // 检查是否重复启动
        if (startedApplications.contains(primarySource)) {
            throw new RuntimeException("Startup failed: The application " + primarySource.getName() + 
                " has already been started");
        }
        
        // 记录该应用已启动
        startedApplications.add(primarySource);

        long startTime = System.currentTimeMillis();
        System.out.println("Starting Evelan Application using Java " + System.getProperty("java.version"));
        
        try {
            // 创建并刷新应用上下文
            // 这里会自动推断 primarySource 所在的包作为扫描的基础包
            ServerApplicationContext context = new ServerApplicationContext(primarySource);
            
            long endTime = System.currentTimeMillis();
            System.out.println("Evelan Application started in " + (endTime - startTime) / 1000.0 + " seconds");
            
            return context;
        } catch (Exception e) {
            // 如果启动失败，从已启动集合中移除，允许重试（可选）
            startedApplications.remove(primarySource);
            throw e;
        }
    }
}
