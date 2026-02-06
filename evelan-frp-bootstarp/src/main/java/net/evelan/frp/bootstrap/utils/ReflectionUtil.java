package net.evelan.frp.bootstrap.utils;

import net.evelan.frp.bootstrap.core.context.ClassPathScanner;

import java.lang.annotation.Annotation;
import java.util.HashSet;
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
            // 先拿到包里面所有的类
            Set<Class<?>> allClasses = ClassPathScanner.getPackageAllClasses(packageName, recursive);
            // 从所有类中过滤出被指定注解标记的类
            return allClasses.stream()
                    .filter(clazz -> annotationClass.stream().anyMatch(ann -> isAnnotationPresentOrMetaPresent(clazz, ann)))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException("Scan Annotation Class Fail: " + e.getMessage(), e);
        }
    }

    /**
     * 扫描是否被 @EComponent @EController @EService 标记
     * @param clazz 待检测的类
     * @param annotationClass 注解类型目标 { @EComponent, @EController, @EService }
     * @return 是否存在
     */
    private static boolean isAnnotationPresentOrMetaPresent(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        // 只要存在一个注解，那么就通过
        if (clazz.isAnnotation()) return false;
        if (clazz.isAnnotationPresent(annotationClass)) return true;
        for (Annotation declaredAnnotation : clazz.getAnnotations()) {
            if (isMetaAnnotationPresent(declaredAnnotation.annotationType(), annotationClass, new HashSet<>())) return true;
        }
        return false;
    }

        /**
         * 递归检查元注解是否存在目标注解
         * <p />
         * 递归检查当前元注解是否直接或间接标记了目标注解。
         * 如果当前元注解直接或间接地标记了目标注解，则返回 true；否则返回 false。
         * 避免循环引用，通过 visited 集合记录已访问的元注解类型。
         * @param currentAnnotationType 当前检查的元注解类型
         * @param targetAnnotationType 目标注解类型
         * @param visited 已访问的元注解类型集合，用于避免循环引用
         * @return 是否存在目标注解
         */
    private static boolean isMetaAnnotationPresent(Class<? extends Annotation> currentAnnotationType,
                                                   Class<? extends Annotation> targetAnnotationType,
                                                   Set<Class<? extends Annotation>> visited) {
        if (!visited.add(currentAnnotationType)) return false;
        // 存在注解就返回
        if (currentAnnotationType.isAnnotationPresent(targetAnnotationType)) return true;
        for (Annotation metaAnnotation : currentAnnotationType.getAnnotations()) {
            Class<? extends Annotation> metaType = metaAnnotation.annotationType();
            if (metaType == currentAnnotationType) continue;
            if (isMetaAnnotationPresent(metaType, targetAnnotationType, visited)) return true;
        }
        return false;
    }

}
