package net.evelan.frp.bootstrap.utils;

import net.evelan.frp.bootstrap.annotation.bean.EComponent;
import net.evelan.frp.bootstrap.annotation.bean.EConfiguration;
import net.evelan.frp.bootstrap.annotation.lifecycle.EPostConstruct;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ClassUtils {

    /**
     * 获取beanName
     * 要注意，理论上传递过来的所有类都是@EComponent的派生类
     * @param clazz 需要查找的类
     * @return beanName
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        // 先判断是不是@EComponent注解的类
        EComponent eComponent = clazz.getAnnotation(EComponent.class);
        if (eComponent != null) {
            // 先去@EComponoent里面找，找到了直接拿value值
            name = eComponent.value();
        } else {
            // 不是@EComponent注解的类，那么继续再其他注解里面查找@EComponent
            // 如果不是@EComponent，那么就是@EController、@EConfiguration、@EService，不管怎么样都是@EComponent的派生，所以一定能找到@EComponent
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(EComponent.class)) {
                    try {
                        // 找到的话就拿value值然后注入到name中
                        name = (String) annotation.annotationType().getMethod("value").invoke(annotation);
                    } catch (Exception e) {
                        throw new RuntimeException("@EComponent is not found in " + clazz.getName(), e);
                    }
                }
            }
        }

        // 如果name还是空字符串 那么就让首字母小写后作为beanName 保持和Spring一致
        if (name.isEmpty()) {
            name = clazz.getSimpleName();
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
        }
        return name;
    }


    /**
     * 获取@EPostConstruct 和 @EPreDestroy 注解的方法
     * @param clazz
     * @param annoClass
     * @return
     */
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass) {
        List<Method> ms = Arrays.stream(clazz.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(annoClass)).map(m -> {
            // @EPostConstruct 和 @EPreDestroy 不能有args 如果写了参数要报错
            if (m.getParameterCount() != 0)
                throw new RuntimeException(String.format("Method '%s' with @%s must not have argument: %s", m.getName(), annoClass.getSimpleName(), clazz.getName()));
            return m;
        }).toList();

        if (ms.isEmpty())
            return null;
        if (ms.size() == 1)
            return ms.get(0);
        throw new RuntimeException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));
    }

    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        return null;
    }
}
