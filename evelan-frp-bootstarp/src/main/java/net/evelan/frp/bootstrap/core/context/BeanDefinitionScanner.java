package net.evelan.frp.bootstrap.core.context;

import net.evelan.frp.bootstrap.annotation.bean.EBean;
import net.evelan.frp.bootstrap.annotation.bean.EComponent;
import net.evelan.frp.bootstrap.annotation.bean.EConfiguration;
import net.evelan.frp.bootstrap.annotation.bean.EController;
import net.evelan.frp.bootstrap.annotation.bean.EService;
import net.evelan.frp.bootstrap.annotation.lifecycle.EPostConstruct;
import net.evelan.frp.bootstrap.annotation.lifecycle.EPreDestroy;
import net.evelan.frp.bootstrap.annotation.lifecycle.EPrimary;
import net.evelan.frp.bootstrap.core.deprecated.AssembleApplicationContext;
import net.evelan.frp.bootstrap.utils.ClassUtils;
import net.evelan.frp.bootstrap.utils.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bean 定义扫描器
 * <p>
 * 负责扫描类路径下的 Bean 类，并创建相应的 BeanDefinition。
 * 支持扫描 @EComponent, @EService, @EController, @EConfiguration 等注解。
 * 同时也支持扫描 @EConfiguration 类中的 @EBean 方法。
 */
public class BeanDefinitionScanner {

    private final AnnotationConfigApplicationContext context;

    public BeanDefinitionScanner(AnnotationConfigApplicationContext context) {
        this.context = context;
    }

    /**
     * 执行扫描并创建 BeanDefinition
     *
     * @return BeanDefinition 映射表 { beanName : BeanDefinition }
     */
    public Map<String, BeanDefinition> scan() {
        Set<Class<?>> beanClasses = collectBeanClasses();
        return createBeanDefinitions(beanClasses);
    }

    private Set<Class<?>> collectBeanClasses() {
        // TODO: 这里扫描包的逻辑可以优化，目前硬编码了获取包名的方式
        String[] currentUtilClassAllClassnames = AssembleApplicationContext.class.getPackageName().split("\\.");
        // 获取所有被 @EService @Component @EController @EConfiguration 注解标记的类
        return ReflectionUtil.findAnnotatedClasses(
                currentUtilClassAllClassnames[0], // 从最顶部的包开始往下扫
                List.of(EController.class, EComponent.class, EService.class, EConfiguration.class),
                true
        );
    }

    private Map<String, BeanDefinition> createBeanDefinitions(Set<Class<?>> beanClasses) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        for (Class<?> clazz : beanClasses) {
            if (isInvalidBeanClass(clazz)) {
                continue;
            }

            // 创建类级别的 BeanDefinition
            createClassBeanDefinition(clazz, defs);

            // 扫描 @EConfiguration 类中的工厂方法
            EConfiguration eConfiguration = ClassUtils.findAnnotation(clazz, EConfiguration.class);
            if (eConfiguration != null) {
                scanFactoryMethods(ClassUtils.getBeanName(clazz), clazz, defs);
            }
        }
        return defs;
    }

    private boolean isInvalidBeanClass(Class<?> clazz) {
        return clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord();
    }

    private void createClassBeanDefinition(Class<?> clazz, Map<String, BeanDefinition> defs) {
        String beanName = ClassUtils.getBeanName(clazz);
        Constructor<?> cons = context.getSuitableConstructor(clazz);

        BeanDefinition beanDefinition = new BeanDefinition(
                beanName,
                clazz,
                cons,
                context.getOrder(clazz),
                clazz.isAnnotationPresent(EPrimary.class),
                null,
                null,
                ClassUtils.findAnnotationMethod(clazz, EPostConstruct.class),
                ClassUtils.findAnnotationMethod(clazz, EPreDestroy.class)
        );
        
        addBeanDefinition(defs, beanDefinition);
    }

    private void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            EBean bean = method.getAnnotation(EBean.class);
            if (bean != null) {
                validateFactoryMethod(clazz, method);
                
                Class<?> beanClass = method.getReturnType();
                BeanDefinition def = new BeanDefinition(
                        ClassUtils.getBeanName(method),
                        beanClass,
                        factoryBeanName,
                        method,
                        context.getOrder(method),
                        method.isAnnotationPresent(EPrimary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null,
                        null
                );
                addBeanDefinition(defs, def);
            }
        }
    }

    private void validateFactoryMethod(Class<?> clazz, Method method) {
        int mod = method.getModifiers();
        if (Modifier.isAbstract(mod)) {
            throw new RuntimeException("@EBean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
        }
        if (Modifier.isFinal(mod)) {
            throw new RuntimeException("@EBean method " + clazz.getName() + "." + method.getName() + " must not be final.");
        }
        if (Modifier.isPrivate(mod)) {
            throw new RuntimeException("@EBean method " + clazz.getName() + "." + method.getName() + " must not be private.");
        }
        Class<?> returnType = method.getReturnType();
        if (returnType.isPrimitive()) {
            throw new RuntimeException("@EBean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
        }
        if (returnType == void.class || returnType == Void.class) {
            throw new RuntimeException("@EBean method " + clazz.getName() + "." + method.getName() + " must not return void.");
        }
    }

    private void addBeanDefinition(Map<String, BeanDefinition> maps, BeanDefinition bean) {
        if (maps.put(bean.getName(), bean) != null) {
            throw new RuntimeException("Duplicate bean name: " + bean.getName());
        }
    }
}
