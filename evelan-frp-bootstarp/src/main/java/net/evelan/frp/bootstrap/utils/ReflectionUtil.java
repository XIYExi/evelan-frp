package net.evelan.frp.bootstrap.utils;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 反射工具类
 */
public class ReflectionUtil {


    /**
     * 查找包中所有被特定注解标记的类
     * @param packageName 要扫描的包名
     * @param annotationClass 注解类型
     * @param recursive 是否递归扫描子包
     * @return 被扫描的集合
     */
    public static Set<Class<?>> findAnnotatedClasses(String packageName,
                                                     List<Class<? extends Annotation>> annotationClass,
                                                     boolean recursive) {
        try {
            Set<Class<?>> allClasses = ClassPathScanner.getPackageAllClasses(packageName, recursive);
            // 过滤出被 @EBean @Component @EController 注解标记的类
            return allClasses.stream().filter(clazz -> annotationClass.stream().anyMatch(clazz::isAnnotationPresent)).collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException("Scan Annotation Class Fail: " + e.getMessage(), e);
        }
    }

}
