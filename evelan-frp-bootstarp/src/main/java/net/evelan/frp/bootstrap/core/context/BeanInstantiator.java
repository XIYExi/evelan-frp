package net.evelan.frp.bootstrap.core.context;

import net.evelan.frp.bootstrap.annotation.lifecycle.EImport;
import net.evelan.frp.bootstrap.annotation.lifecycle.EValue;
import net.evelan.frp.bootstrap.core.solver.PropertyResolver;
import net.evelan.frp.bootstrap.utils.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

/**
 * Bean 实例化器
 * <p>
 * 负责通过构造函数或工厂方法创建 Bean 实例。
 * 自动处理构造函数/工厂方法参数的依赖注入。
 */
public class BeanInstantiator {

    private final AnnotationConfigApplicationContext context;
    private final PropertyResolver propertyResolver;

    public BeanInstantiator(AnnotationConfigApplicationContext context, PropertyResolver propertyResolver) {
        this.context = context;
        this.propertyResolver = propertyResolver;
    }

    /**
     * 创建 Bean 实例（早期单例，尚未进行属性注入）
     *
     * @param def Bean 定义
     * @return Bean 实例
     */
    public Object createBean(BeanDefinition def) {
        // 创建方式：构造方法或者工厂方法
        Executable createFn;
        if (def.getFactoryName() == null) {
            // 通过构造方法创建
            createFn = def.getConstructor();
        } else {
            // 通过工厂方法创建
            createFn = def.getFactoryMethod();
        }

        // 解析参数
        Object[] args = resolveParameters(def, createFn);

        // 创建实例
        Object instance;
        try {
            if (def.getFactoryName() == null) {
                // 通过构造方法实例化
                instance = def.getConstructor().newInstance(args);
            } else {
                // 通过工厂方法实例化
                Object configInstance = context.getBean(def.getFactoryName());
                instance = def.getFactoryMethod().invoke(configInstance, args);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
        }

        return instance;
    }

    /**
     * 解析构造函数或工厂方法的参数
     */
    private Object[] resolveParameters(BeanDefinition def, Executable createFn) {
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parametersAnnos = createFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            final Parameter param = parameters[i];
            final Annotation[] paramAnnos = parametersAnnos[i];
            
            // 查找 @EValue 或 @EImport 注解
            final EValue eValue = ClassUtils.getAnnotation(paramAnnos, EValue.class);
            final EImport eImport = ClassUtils.getAnnotation(paramAnnos, EImport.class);

            final boolean isConfiguration = context.isConfigurationDefinition(def);
            
            // 校验参数注解规则
            validateParameterAnnotations(def, isConfiguration, eValue, eImport);

            final Class<?> type = param.getType();
            if (eValue != null) {
                // 参数是 @EValue 类型，从 PropertyResolver 获取配置值
                args[i] = this.propertyResolver.getRequiredProperty(eValue.value(), type);
            } else {
                // 参数是 @EImport 类型，从容器获取依赖 Bean
                String name = eImport.value();
                boolean required = eImport.isRequired();
                
                // 查找依赖的 BeanDefinition
                BeanDefinition dependsOnDef = name.isEmpty() ? context.findBeanDefinition(type) : context.findBeanDefinition(name, type);
                
                if (required && dependsOnDef == null) {
                    throw new RuntimeException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName()));
                }
                
                if (dependsOnDef != null) {
                    // 获取依赖的 Bean 实例
                    Object dependsOnBeanInstance = dependsOnDef.getInstance();
                    if (dependsOnBeanInstance == null && !isConfiguration) {
                        // 如果依赖的 Bean 尚未初始化，递归调用初始化
                        dependsOnBeanInstance = context.createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = dependsOnBeanInstance;
                } else {
                    args[i] = null;
                }
            }
        }
        return args;
    }

    private void validateParameterAnnotations(BeanDefinition def, boolean isConfiguration, EValue eValue, EImport eImport) {
        if (isConfiguration && eImport != null) {
            throw new RuntimeException(
                    String.format("Cannot specify @EImport when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
        }

        if (eValue != null && eImport != null) {
            throw new RuntimeException(
                    String.format("Cannot specify both @EImport and @EValue when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
        }

        if (eValue == null && eImport == null) {
            throw new RuntimeException(
                    String.format("Must specify @EImport or @EValue when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
        }
    }
}
