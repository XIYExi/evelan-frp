package net.evelan.frp.bootstrap.core.context;

import net.evelan.frp.bootstrap.annotation.bean.EConfiguration;
import net.evelan.frp.bootstrap.annotation.lifecycle.EOrder;
import net.evelan.frp.bootstrap.core.core.ApplicationContextUtils;
import net.evelan.frp.bootstrap.core.core.ConfigurableApplicationContext;
import net.evelan.frp.bootstrap.core.solver.PropertyResolver;
import net.evelan.frp.bootstrap.utils.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于注解配置的应用上下文
 * <p>
 * 核心容器类，负责 Bean 的扫描、定义、实例化、依赖注入和生命周期管理。
 * 模拟了 Spring 的 IOC 容器行为。
 */
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {
    
    protected final PropertyResolver propertyResolver;
    protected final Map<String, BeanDefinition> beans;
    
    // 依赖注入器
    private final DependencyInjector dependencyInjector;
    // Bean 实例化器
    private final BeanInstantiator beanInstantiator;
    // Bean 定义扫描器
    private final BeanDefinitionScanner beanScanner;

    // 正在创建中的 Bean 名称集合，用于检测循环依赖（主要针对构造器注入）
    private final Set<String> creatingBeanNames;
    
    // Bean 后置处理器列表
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        this.dependencyInjector = new DependencyInjector(this, propertyResolver);
        this.beanInstantiator = new BeanInstantiator(this, propertyResolver);
        this.beanScanner = new BeanDefinitionScanner(this);
        // 1. 扫描并注册 Bean 定义
        this.beans = this.beanScanner.scan(); // 初始化 beans map
        this.creatingBeanNames = new HashSet<>();
        // 执行容器刷新，加载所有 Bean
        refresh();
    }

    /**
     * 刷新容器，执行核心加载流程
     */
    public void refresh() {

        // 2. 注册 BeanPostProcessor
        registerBeanPostProcessors();

        // 3. 实例化 Bean (分为 Configuration 和 普通 Bean)
        // 先实例化 @EConfiguration，因为它们可能包含工厂方法
        createEConfigurationBeans();
        // 再实例化其他 Bean
        createNormalBeans();

        // 4. 依赖注入 (属性填充)
        injectBeans();

        // 5. 初始化 Bean (调用 @PostConstruct 等)
        initBeans();
    }

    /**
     * 注册 BeanPostProcessor
     * 查找实现了 BeanPostProcessor 接口的 BeanDefinition，实例化并添加到列表中
     */
    private void registerBeanPostProcessors() {
        List<BeanDefinition> processors = this.beans.values().stream()
                .filter(def -> BeanPostProcessor.class.isAssignableFrom(def.getBeanClass()))
                .sorted(Comparator.comparingInt(BeanDefinition::getOrder))
                .toList();

        for (BeanDefinition def : processors) {
            // 确保 BeanPostProcessor 被实例化
            if (def.getInstance() == null) {
                createBeanAsEarlySingleton(def);
            }
            // 注入依赖（BeanPostProcessor 也可能依赖其他 Bean）
            dependencyInjector.inject(def);
            // 初始化 BeanPostProcessor (调用 @PostConstruct)
            initBean(def);
            
            // 添加到列表
            Object instance = def.getInstance();
            if (instance instanceof BeanPostProcessor processor) {
                this.beanPostProcessors.add(processor);
            }
        }
    }

    /**
     * 初始化所有 Bean
     * 调用 @PostConstruct 方法和 BeanPostProcessor
     */
    private void initBeans() {
        this.beans.values().forEach(this::initBean);
    }

    private void initBean(BeanDefinition def) {
        Object bean = def.getInstance();
        
        // BeanPostProcessor Before Initialization (暂未实现，可扩展)
        
        // 调用初始化方法
        callMethod(bean, def.getInitMethod(), def.getInitMethodName());
        
        // BeanPostProcessor After Initialization (暂未实现，可扩展)
    }

    /**
     * 调用指定的方法（如 init 或 destroy）
     */
    private void callMethod(Object beanInstance, Method method, String namedMethod) {
        if (method != null) {
            try {
                method.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to invoke init/destroy method", e);
            }
        } else if (namedMethod != null) {
            Method named = ClassUtils.getNamedMethod(beanInstance.getClass(), namedMethod);
            named.setAccessible(true);
            try {
                named.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to invoke named init/destroy method: " + namedMethod, e);
            }
        }
    }

    /**
     * 执行依赖注入
     */
    private void injectBeans() {
        this.beans.values().forEach(dependencyInjector::inject);
    }

    /**
     * 创建 @EConfiguration 类型的 Bean
     */
    private void createEConfigurationBeans() {
        this.beans.values().stream()
                .filter(this::isConfigurationDefinition)
                .sorted()
                .forEach(this::createBeanAsEarlySingleton);
    }

    /**
     * 创建普通类型的 Bean
     */
    private void createNormalBeans() {
        List<BeanDefinition> defs = this.beans.values().stream()
                .filter(def -> def.getInstance() == null)
                .sorted()
                .toList();
        
        defs.forEach(def -> {
            if (def.getInstance() == null) {
                createBeanAsEarlySingleton(def);
            }
        });
    }

    /**
     * 创建 Bean 实例（早期单例）
     * 如果存在循环依赖，会在 creatingBeanNames 中检测到
     */
    @Override
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        // System.out.printf("Creating bean '%s': %s%n", def.getName(), def.getBeanClass().getName());
        
        if (!this.creatingBeanNames.add(def.getName())) {
            throw new RuntimeException(String.format("Circular dependency detected when create bean '%s'", def.getName()));
        }

        Object instance = beanInstantiator.createBean(def);
        def.setInstance(instance);
        
        // 创建完成后移除正在创建标记
        this.creatingBeanNames.remove(def.getName());
        
        return instance;
    }

    @Override
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new RuntimeException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) getProxiedInstance(def);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if (t == null) {
            throw new RuntimeException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType.getName()));
        }
        return t;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new RuntimeException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) getProxiedInstance(def);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> defs = findBeanDefinitions(requiredType);
        if (defs.isEmpty()) {
            return List.of();
        }
        return defs.stream()
                .map(def -> (T) getProxiedInstance(def))
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        this.beans.values().forEach(def -> {
            final Object beanInstance = getProxiedInstance(def);
            callMethod(beanInstance, def.getDestroyMethod(), def.getDestroyMethodName());
        });
        this.beans.clear();
        ApplicationContextUtils.setApplicationContext(null);
    }

    /**
     * 获取代理后的 Bean 实例
     * 应用 BeanPostProcessor 的 postProcessOnSetProperty 逻辑（如果有）
     */
    private Object getProxiedInstance(BeanDefinition def) {
        Object beanInstance = def.getInstance();
        // 这里的逻辑保留了原有的设计：反向遍历 Processor
        // 注意：通常 BeanPostProcessor 是在初始化前后应用，而不是在 getBean 时。
        // 但为了保持原有逻辑的兼容性（可能用于动态代理替换），这里保留。
        List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
        Collections.reverse(reversedBeanPostProcessors);
        
        for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanInstance, def.getName());
            if (restoredInstance != beanInstance) {
                beanInstance = restoredInstance;
            }
        }
        return beanInstance;
    }

    // --- 查找辅助方法 ---

    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) getProxiedInstance(def);
    }

    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) getProxiedInstance(def);
    }

    @Override
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null)
            return null;
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new RuntimeException(
                    String.format(
                            "Autowire required type '%s' but bean '%s' has actual type '%s'.",
                            requiredType.getName(),
                            name,
                            def.getBeanClass().getName()
                    )
            );
        }
        return def;
    }

    public BeanDefinition findBeanDefinition(String name) {
        return beans.get(name);
    }

    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        if (defs.isEmpty())
            return null;
        if (defs.size() == 1)
            return defs.get(0);
        List<BeanDefinition> primaryDefs = defs.stream().filter(BeanDefinition::isPrimary).toList();
        if (primaryDefs.size() == 1)
            return primaryDefs.get(0);
        if (primaryDefs.isEmpty())
            throw new RuntimeException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        else
            throw new RuntimeException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
    }

    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream().filter(def -> type.isAssignableFrom(def.getBeanClass())).sorted().toList();
    }

    // --- 内部辅助方法 ---

    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), EConfiguration.class) != null;
    }

    int getOrder(Class<?> clazz) {
        EOrder annotation = clazz.getAnnotation(EOrder.class);
        return annotation != null ? annotation.value() : Integer.MAX_VALUE;
    }

    int getOrder(Method method) {
        EOrder annotation = method.getAnnotation(EOrder.class);
        return annotation != null ? annotation.value() : Integer.MAX_VALUE;
    }

    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1)
                throw new RuntimeException("More than one constructor found in class: " + clazz.getName());
        }
        if (cons.length != 1)
            throw new RuntimeException("More than one constructor found in class: " + clazz.getName());
        return cons[0];
    }
}
