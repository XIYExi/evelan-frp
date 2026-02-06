package net.evelan.frp.bootstrap.core;


import net.evelan.frp.bootstrap.annotation.core.EComponent;
import net.evelan.frp.bootstrap.annotation.core.EConfiguration;
import net.evelan.frp.bootstrap.annotation.core.EController;
import net.evelan.frp.bootstrap.annotation.core.EService;
import net.evelan.frp.bootstrap.utils.ReflectionUtil;

import java.util.List;
import java.util.Set;

/**
 * 反射检索所有的标记类
 */
public class ResourceResolver {

    private final String basePackage;

    public ResourceResolver() {
        this(ResourceResolver.class.getPackageName().split("\\.")[0]);
    }
    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }
    public ResourceResolver(Class<?> primarySource) {
        this(primarySource.getPackageName());
    }


    /**
     * 通过反射扫描出所有被注解标记的类
     * @return 被标记的类 | null
     */
    public Set<Class<?>> scan() {
        Set<Class<?>> annClasses = collectBeanObject();
        if (!annClasses.isEmpty())
            return annClasses;
        return null;
    }


    /**
     * 扫描出所有被注解标记的类
     * @return 被标记的类
     */
    private Set<Class<?>> collectBeanObject() {
        // 使用配置的basePackage
        System.out.println("Scanning package: " + basePackage);

        // 获取所有被 @EService @Component @EController @EConfiguration 注解标记的类
        Set<Class<?>> annotatedClasses = ReflectionUtil.findAnnotatedClasses(
                basePackage,
                List.of(EController.class, EComponent.class, EService.class, EConfiguration.class),
                // List.of(EComponent.class),
                true
        );
        annotatedClasses.forEach(clazz -> {
            System.out.println("Found bean class: " + clazz.getName());
        });
        return annotatedClasses;
    }

}
