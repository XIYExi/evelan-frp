package net.evelan.frp.bootstrap.core.context;

import net.evelan.frp.bootstrap.annotation.bean.*;
import net.evelan.frp.bootstrap.annotation.lifecycle.*;
import net.evelan.frp.bootstrap.core.core.ApplicationContextUtils;
import net.evelan.frp.bootstrap.core.core.ConfigurableApplicationContext;
import net.evelan.frp.bootstrap.core.deprecated.AssembleApplicationContext;
import net.evelan.frp.bootstrap.core.solver.PropertyResolver;
import net.evelan.frp.bootstrap.utils.ClassUtils;
import net.evelan.frp.bootstrap.utils.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {
    protected final PropertyResolver propertyResolver;
    protected final Map<String, BeanDefinition> beans;

    // ioc扫描的时候检测循环依赖
    private Set<String> creatingBeanNames;
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的class类型
        final Set<Class<?>> beanClassNames = collectBeanObject();
        // 创建bean的定义
        this.beans = createBeanDefinitions(beanClassNames);
        // 循环依赖检测
        this.creatingBeanNames = new HashSet<>();
        // 先解决强依赖（@EConfiguration注入）
        createEConfigurationBeans();
        // 再创建其他普通的EBean
        createNormalBeans();
        // 通过字段和set方法注入依赖
        injectBean();
        // 调用init方法
    }

    void init() {
        this.beans.values().forEach(def -> {
            initBean(def);
        });
    }

    void initBean(BeanDefinition def) {
        callMethod(def.getInstance(), def.getInitMethod(), def.getInitMethodName());
    }

    private void callMethod(Object beanInstance, Method method, String namedMethod) {
        // 调用init/destroy方法:
        if (method != null) {
            try {
                method.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else if (namedMethod != null) {
            // 查找initMethod/destroyMethod="xyz"，注意是在实际类型中查找:
            Method named = ClassUtils.getNamedMethod(beanInstance.getClass(), namedMethod);
            named.setAccessible(true);
            try {
                named.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void injectBean() {
        this.beans.values().forEach(def -> {
            injectBean(def);
        });
    }

    /**
     * 注入依赖，但是不调用init方法
     * @param def
     */
    void injectBean(BeanDefinition def) {
        try {
            injectProperties(def, def.getBeanClass(), def.getInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws Exception {
        // 在当前类查查找Field和Method并注入
        for (Field f : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, m);
        }
        // 在父类中查找Field和Method并尝试注入
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            injectProperties(def, superClazz, bean);
        }
    }

    /**
     * 尝试注入依赖
     * @param def
     * @param clazz
     * @param bean
     * @param acc
     * @throws Exception
     */
    void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws Exception {
        EValue value = acc.getAnnotation(EValue.class);
        EImport eImport = acc.getAnnotation(EImport.class);
        if (value == null && eImport == null)
            return;
        Field field = null;
        Method method = null;
        if (acc instanceof Field f) {
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method m) {
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
            throw new RuntimeException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        // @EValue注入:
        if (value != null) {
            Object propValue = this.propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                field.set(bean, propValue);
            }
            if (method != null) {
                method.invoke(bean, propValue);
            }
        }

        // @EImport注入:
        if (eImport != null) {
            String name = eImport.value();
            boolean required = eImport.isRequired();
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);
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

    void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new RuntimeException("Cannot inject static field: " + m);
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field field) {
                throw new RuntimeException("Cannot inject final field: " + field);
            }
            if (m instanceof Method method) {
                System.err.println("Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.");
            }
        }
    }


    /**
     * 创建其余的EBean对象
     */
    void createNormalBeans() {
        // 获取所有还没有创建的BeanDefinitions列表
        List<BeanDefinition> defs = this.beans.values().stream().filter(def -> def.getInstance() == null).sorted().toList();
        defs.forEach(def -> {
            if (def.getInstance() == null) {
                // 创建EBean
                createBeanAsEarlySingleton(def);
            }
        });
    }

    /**
     * 创建EConfiguration对象中的@EBean和@EValue对象
     */
    void createEConfigurationBeans() {
        this.beans
                .values()
                .stream()
                .filter(this::isConfigurationDefinition)
                .sorted()
                .forEach(this::createBeanAsEarlySingleton);
    }

    /**
     * 创建一个EBean对象，但不进行字段和方法级别的注入。
     * 如果创建的EBean不是EConfiguration，则再构造方法中注入的依赖EBean会自动创建
     *
     * @param def
     */
    @Override
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        System.out.printf("Try create bean '%s' as early singleton: %s%n", def.getName(), def.getBeanClass().getName());
        if (!this.creatingBeanNames.add(def.getName()))
            throw new RuntimeException(String.format("Circular dependency detected when create bean '%s'", def.getName()));

        // 创建方式：构造方法或者工厂方法
        Executable createFn = null;
        if (def.getFactoryName() == null)
            // 通过构造方法创建
            createFn = def.getConstructor();
        else
            // 通过工厂方法创建
            createFn = def.getFactoryMethod();

        // 创建参数
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parametersAnnos = createFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final Parameter param = parameters[i];
            final Annotation[] paramAnnos = parametersAnnos[i];
            // 判断每个参数是不是被@EValue或者@EImport标记，如果被标记了那么就是需要注入的对象，需要去ioc容器里面拿值
            final EValue eValue = ClassUtils.getAnnotation(paramAnnos, EValue.class);
            final EImport eImport = ClassUtils.getAnnotation(paramAnnos, EImport.class);

            // @EConfiguration 类型的bean是工厂模式，不允许使用@EImport创建
            final boolean isConfiguration = isConfigurationDefinition(def);
            if (isConfiguration && eImport != null)
                throw new RuntimeException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName()));

            // 参数只能是@EValue或者@EImport两者之一
            if (eValue != null && eImport != null)
                throw new RuntimeException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));

            if (eValue == null && eImport == null)
                throw new RuntimeException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));

            // 参数类型
            final Class<?> type = param.getType();
            if (eValue != null)
                // 参数是@EValue类型
                args[i] = this.propertyResolver.getRequiredProperty(eValue.value(), type);
            else {
                // 参数是@EImport类型
                String name = eImport.value();
                boolean required = eImport.isRequired();
                // 依赖的BeanDefinition
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                // 检测required==true
                if (required && dependsOnDef == null) {
                    throw new RuntimeException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName()));
                }
                if (dependsOnDef != null) {
                    // 获取依赖的EBean
                    Object eImportBeanInstance = dependsOnDef.getInstance();
                    if (eImportBeanInstance == null && !isConfiguration) {
                        // 当前依赖Bean尚未初始化，递归调用初始化该依赖Bean:
                        eImportBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = eImportBeanInstance;
                } else
                    args[i] = null;
            }

        }

        // 创建实例
        Object instance = null;
        if (def.getFactoryName() == null) {
            // 通过构造方法注入
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        } else {
            // 通过@EBean方法创建
            Object configInstance = getBean(def.getFactoryName());
            try {
                instance = def.getFactoryMethod().invoke(configInstance, args);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }
        def.setInstance(instance);
        return def.getInstance();
    }


    @Override
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    /**
     * 通过Name查找Bean，不存在时抛出异常
     *
     * @param name
     * @param <T>
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public  <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null)
            throw new RuntimeException(String.format("No bean defined with name '%s'.", name));
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过Name和Type查找Bean，不存在抛出异常
     *
     * @param name
     * @param requiredType
     * @param <T>
     * @return
     */
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if (t == null)
            throw new RuntimeException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType.getName()));
        return t;
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new RuntimeException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    @Override
 @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> defs = findBeanDefinitions(requiredType);
        if (defs.isEmpty()) {
            return List.of();
        }
        List<T> list = new ArrayList<>(defs.size());
        for (var def : defs) {
            list.add((T) def.getRequiredInstance());
        }
        return list;
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

    private Object getProxiedInstance(BeanDefinition def) {
         Object beanInstance = def.getInstance();
        // 如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean:
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


    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }


    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }


    @SuppressWarnings("unchecked")
    protected <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefinitions(requiredType).stream().map(def -> (T) def.getRequiredInstance()).toList();
    }


    /**
     * 【IoC】
     * 扫描所有的Bean对象到ioc容器中
     *
     * @param beanClassNames 所有被标记的类
     * @return BeanDefinition字典 { beanName : BeanDefinition }
     */
    private Map<String, BeanDefinition> createBeanDefinitions(Set<Class<?>> beanClassNames) {
        /**
         * 第一步，先扫描所有的 bean 对象，也就是@EComponent，@EService，@EController，@EConfiguration
         * 为其创建BeanDefinition并存储在ioc容器（beans）中
         */
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

            /**
             * 第二步，进入到每个@EConfiguration注解中，检查内部是否有 @EBean 对象，如果有同样创建BeanDefinition并注入容器
             */
            EConfiguration eConfiguration = ClassUtils.findAnnotation(clazz, EConfiguration.class);
            if (eConfiguration != null) {
                scanFactoryMethods(beanName, clazz, defs);
            }
        }
        return defs;
    }

    /**
     * 扫描工厂方法
     *
     * @param factoryBeanName
     * @param clazz
     * @param defs
     */
    private void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            EBean bean = method.getAnnotation(EBean.class);
            if (bean != null) {
                // 获取工厂方法
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new RuntimeException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) {
                    throw new RuntimeException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new RuntimeException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }
                Class<?> beanClass = method.getReturnType();
                if (beanClass.isPrimitive()) {
                    throw new RuntimeException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
                }
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new RuntimeException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }

                BeanDefinition def = new BeanDefinition(
                        ClassUtils.getBeanName(method),
                        beanClass,
                        factoryBeanName,
                        method,
                        getOrder(method),
                        method.isAnnotationPresent(EPrimary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null,
                        null
                );
                addBeanDefinitions(defs, def);
                System.err.println("define bean: " + def);
            }
        }
    }


    /**
     * 根据Name和Type查找BeanDefinition，如果Name不存在，返回null，如果Name存在，但Type不匹配，抛出异常。
     *
     * @param name         BeanName
     * @param requiredType BeanClass
     * @return BeanDefinition
     */
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

    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), EConfiguration.class) != null;
    }

    /**
     * 根据Name查找BeanDefinition，如果Name不存在，返回null
     *
     * @param name BeanName
     * @return BeanDefinition
     */
    public BeanDefinition findBeanDefinition(String name) {
        return beans.get(name);
    }

    /**
     * 根据Type查找BeanDefinition，返回所有匹配的BeanDefinition
     *
     * @param type BeanClass
     * @return List<BeanDefinition>
     */
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream().filter(def -> type.isAssignableFrom(def.getBeanClass())).sorted().toList();
    }

    /**
     * 根据Type查找某个BeanDefinition，如果不存在返回null，
     * 如果存在多个返回@Primary标注的一个，
     * 如果有多个@Primary标注，或没有@Primary标注但找到多个，均抛出异常
     *
     * @param type BeanClass
     * @return BeanDefinition
     */
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


    int getOrder(Class<?> clazz) {
        EOrder annotation = clazz.getAnnotation(EOrder.class);
        return annotation != null ? annotation.value() : Integer.MAX_VALUE;
    }

    int getOrder(Method method) {
        EOrder annotation = method.getAnnotation(EOrder.class);
        return annotation != null ? annotation.value() : Integer.MAX_VALUE;
    }

    /**
     * 将BeanDefinition添加到容器中
     *
     * @param maps ioc容器
     * @param bean BeanDefinition对象
     */
    void addBeanDefinitions(Map<String, BeanDefinition> maps, BeanDefinition bean) {
        // 以beanName作为key，将BeanDefinition对象存储在容器中
        if (maps.put(bean.getName(), bean) != null)
            throw new RuntimeException("Duplicate bean name: " + bean.getName());
    }


    /**
     * 获取合适的构造函数
     *
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
