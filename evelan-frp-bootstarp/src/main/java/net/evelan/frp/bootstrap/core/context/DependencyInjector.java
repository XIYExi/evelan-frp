package net.evelan.frp.bootstrap.core.context;

import net.evelan.frp.bootstrap.annotation.lifecycle.EImport;
import net.evelan.frp.bootstrap.annotation.lifecycle.EValue;
import net.evelan.frp.bootstrap.core.solver.PropertyResolver;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 依赖注入器
 * <p>
 * 负责处理 Bean 的属性注入（Field 和 Method 注入）。
 * 支持 {@link EValue} 和 {@link EImport} 注解。
 */
public class DependencyInjector {

    private final EvelanConfigApplicationContext context;
    private final PropertyResolver propertyResolver;

    public DependencyInjector(EvelanConfigApplicationContext context, PropertyResolver propertyResolver) {
        this.context = context;
        this.propertyResolver = propertyResolver;
    }

    /**
     * 注入 Bean 的依赖
     *
     * @param def Bean 定义
     */
    public void inject(BeanDefinition def) {
        try {
            injectProperties(def, def.getBeanClass(), def.getInstance());
        } catch (Exception e) {
            throw new RuntimeException("Dependency injection failed for bean: " + def.getName(), e);
        }
    }

    /**
     * 递归注入属性（包括父类）
     */
    private void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws Exception {
        // 在当前类查找 Field 和 Method 并注入
        for (Field f : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, m);
        }
        // 在父类中查找 Field 和 Method 并尝试注入
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null && superClazz != Object.class) {
            injectProperties(def, superClazz, bean);
        }
    }

    /**
     * 尝试注入单个属性或方法
     */
    private void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws Exception {
        EValue value = acc.getAnnotation(EValue.class);
        EImport eImport = acc.getAnnotation(EImport.class);
        if (value == null && eImport == null) {
            return;
        }

        Field field = null;
        Method method = null;

        if (acc instanceof Field f) {
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        } else if (acc instanceof Method m) {
            checkFieldOrMethod(m);
            if (m.getParameters().length != 1) {
                throw new RuntimeException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s", m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];

        if (value != null && eImport != null) {
            throw new RuntimeException(String.format("Cannot specify both @EValue and @EImport when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        // @EValue 注入
        if (value != null) {
            Object propValue = this.propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                field.set(bean, propValue);
            }
            if (method != null) {
                method.invoke(bean, propValue);
            }
        }

        // @EImport 注入
        if (eImport != null) {
            String name = eImport.value();
            boolean required = eImport.isRequired();
            Object depends;
            if (name.isEmpty()) {
                depends = context.findBean(accessibleType);
            } else {
                depends = context.findBean(name, accessibleType);
            }

            if (required && depends == null) {
                throw new RuntimeException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s", clazz.getSimpleName(),
                        accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            if (depends != null) {
                if (field != null) {
                    field.set(bean, depends);
                }
                if (method != null) {
                    method.invoke(bean, depends);
                }
            }
        }
    }

    /**
     * 检查字段或方法修饰符
     */
    private void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new RuntimeException("Cannot inject static field: " + m);
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field field) {
                throw new RuntimeException("Cannot inject final field: " + field);
            }
            // Final methods in proxies might be an issue, but warning is sufficient for now
        }
    }
}
