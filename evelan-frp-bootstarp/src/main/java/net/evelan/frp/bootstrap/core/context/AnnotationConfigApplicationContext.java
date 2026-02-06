package net.evelan.frp.bootstrap.core.context;

import net.evelan.frp.bootstrap.annotation.bean.EComponent;
import net.evelan.frp.bootstrap.annotation.bean.EConfiguration;
import net.evelan.frp.bootstrap.annotation.bean.EController;
import net.evelan.frp.bootstrap.annotation.bean.EService;
import net.evelan.frp.bootstrap.annotation.lifecycle.EOrder;
import net.evelan.frp.bootstrap.annotation.lifecycle.EPostConstruct;
import net.evelan.frp.bootstrap.annotation.lifecycle.EPreDestroy;
import net.evelan.frp.bootstrap.annotation.lifecycle.EPrimary;
import net.evelan.frp.bootstrap.core.deprecated.AssembleApplicationContext;
import net.evelan.frp.bootstrap.core.solver.PropertyResolver;
import net.evelan.frp.bootstrap.utils.ClassUtils;
import net.evelan.frp.bootstrap.utils.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotationConfigApplicationContext {
    protected final PropertyResolver propertyResolver;
    protected final Map<String, BeanDefinition> beans;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的class类型
        final Set<Class<?>> beanClassNames = collectBeanObject();
        // 创建bean的定义
        this.beans = createBeanDefinitions(beanClassNames);
    }

    private Map<String, BeanDefinition> createBeanDefinitions(Set<Class<?>> beanClassNames) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        for (Class<?> clazz : beanClassNames) {
            // 每一个clazz都是被扫描出来的，被指定注解标记的类
            // 常规检查，正常来说被标记的类不可能是这个类型，但是防止被瞎标记，所以进行安全性校验
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord())
                continue;
            String beanName = ClassUtils.getBeanName(clazz);
            Constructor<?> cons = getSuitableConstructor(clazz);

            BeanDefinition beanDefinition = new BeanDefinition(
                    beanName,
                    clazz,
                    cons,
                    getOrder(clazz),
                    clazz.isAnnotationPresent(EPrimary.class),
                    null,
                    null,
                    ClassUtils.findAnnotationMethod(clazz, EPostConstruct.class),
                    ClassUtils.findAnnotationMethod(clazz, EPreDestroy.class)
            );
            addBeanDefinitions(defs, beanDefinition);

            System.err.println("define bean: " + defs);
            EConfiguration eConfiguration = ClassUtils.findAnnotation(clazz, EConfiguration.class);
        }
    }

    int getOrder(Class<?> clazz) {
        EOrder annotation = clazz.getAnnotation(EOrder.class);
        return annotation != null ? annotation.value() : Integer.MAX_VALUE;
    }

    void addBeanDefinitions(Map<String, BeanDefinition> maps, BeanDefinition bean) {
        if (maps.put(bean.getName(), bean) != null)
            throw new RuntimeException("Duplicate bean name: " + bean.getName());
    }

    /**
     * 获取合适的构造函数
     * @param clazz
     * @return
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            // 默认无参构造
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1)
                throw new RuntimeException("More than one constructor found in class: " + clazz.getName());
        }
        if (cons.length != 1)
            throw new RuntimeException("More than one constructor found in class: " + clazz.getName());
        return cons[0];
    }


    /**
     * 扫描出所有被注解标记的类
     *
     * @return
     */
    private Set<Class<?>> collectBeanObject() {
        String[] currentUtilClassAllClassnames = AssembleApplicationContext.class.getPackageName().split("\\.");
        // 获取所有被 @EService @Component @EController @EConfiguration 注解标记的类
        Set<Class<?>> annotatedClasses = ReflectionUtil.findAnnotatedClasses(
                currentUtilClassAllClassnames[0], // 从最顶部的包开始往下扫
                List.of(EController.class, EComponent.class, EService.class, EConfiguration.class),
                true
        );
        annotatedClasses.forEach(clazz -> {
            System.out.println(clazz.getName());
        });
        return annotatedClasses;
    }
}
