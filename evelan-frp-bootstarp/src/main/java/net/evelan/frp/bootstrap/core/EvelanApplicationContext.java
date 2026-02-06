package net.evelan.frp.bootstrap.core;

import net.evelan.frp.bootstrap.annotation.core.EBean;
import net.evelan.frp.bootstrap.annotation.core.EConfiguration;
import net.evelan.frp.bootstrap.annotation.core.EComponent;
import net.evelan.frp.bootstrap.annotation.core.EController;
import net.evelan.frp.bootstrap.annotation.core.EImport;
import net.evelan.frp.bootstrap.annotation.core.EPostConstruct;
import net.evelan.frp.bootstrap.annotation.core.EService;
import net.evelan.frp.bootstrap.utils.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准的单例工厂模式实现
 * 通过反射来创建Bean对象，通过三个注册表模式（三个Map）来管理Bean对象
 * 实现了IOC的基本功能，Bean创建、依赖注入和生命周期管理
 * 
 * 修改说明：
 * 1. 改为非静态成员变量，支持多实例
 * 2. 增加refresh()方法，支持手动刷新
 * 3. 增加构造函数，支持指定扫描包
 *
 * @version 2.0
 */
public class EvelanApplicationContext {
    /**
     * IOC 容器
     * key: 被扫描对象实现的接口 value：接口实现类
     */
    private final Map<Class<?>, List<Object>> interfaceContainerMap = new ConcurrentHashMap<>();
    /**
     * IOC 容器
     * key: 被扫描的class value: 对象
     */
    private final Map<Class<?>, Object> containerMap = new ConcurrentHashMap<>();
    /**
     * IOC扫描的对象
     * key：对象名字 value：对象
     */
    private final Map<String, Object> iocNameContainerMap = new ConcurrentHashMap<>();

    private final String basePackage;

    /**
     * 创建一个应用上下文，默认扫描当前包的根包
     * 为了兼容旧代码，保持原有逻辑：取EvelanApplicationContext所在包的根包（通常是net）
     */
    public EvelanApplicationContext() {
        // 保持原有逻辑：EvelanApplicationContext.class.getPackageName().split("\\.")[0]
        // 但建议使用带参数的构造函数以获得更精确的扫描范围
        this(EvelanApplicationContext.class.getPackageName().split("\\.")[0]);
    }

    /**
     * 创建一个应用上下文，指定扫描包
     * @param basePackage 扫描包路径
     */
    public EvelanApplicationContext(String basePackage) {
        this.basePackage = basePackage;
        refresh();
    }
    
    /**
     * 创建一个应用上下文，指定主类
     * @param primarySource 主类
     */
    public EvelanApplicationContext(Class<?> primarySource) {
        this(primarySource.getPackageName());
    }

    /**
     * 刷新容器，执行初始化
     */
    public void refresh() {
        // 清理旧数据（如果是重新刷新）
        interfaceContainerMap.clear();
        containerMap.clear();
        iocNameContainerMap.clear();
        
        doInitInstance();
        doInitConfigurationBeans();
        doDI();
        doInitMethods();
    }

    /**
     * 执行初始化方法 @EPostConstruct
     */
    private void doInitMethods() {
        for (Map.Entry<Class<?>, Object> entry : containerMap.entrySet()) {
            Object bean = entry.getValue();
            Class<?> clazz = entry.getKey();
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(EPostConstruct.class)) {
                    try {
                        method.setAccessible(true);
                        method.invoke(bean);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke init method for " + clazz.getName(), e);
                    }
                }
            }
        }
    }


    /**
     * 初始化application上下文容器
     */
    private void doInitInstance() {
        try {
            // 扫描出来被注解标记的所有类
            Set<Class<?>> annClasses = collectBeanObject();
            // 反射给扫描出来的类生成对象
            classObjectGeneration(annClasses);
        } catch (Exception e) {
            System.out.println("初始化失败：" + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * 依赖注入
     */
    private void doDI() {
        Set<Class<?>> classes = containerMap.keySet();
        for (Class<?> clazz : classes) {
            // 得到所有的属性
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                // 看这个注解上面有没有挂@EImport注解
                boolean annotationPresent = declaredField.isAnnotationPresent(EImport.class);
                if (annotationPresent) {
                    EImport importAnnotation = declaredField.getAnnotation(EImport.class);
                    String objectName = importAnnotation.value();
                    Object bean = null;
                    if (!objectName.isEmpty()) {
                        // EImport配置了value，优先按照value找bean对象
                        bean = getBean(objectName);
                        if (bean == null) {
                            throw new RuntimeException("No bean named " + objectName + " available.");
                        }
                    } else {
                        // 如果没有配置value，那么按照类型找
                        Class<?> type = declaredField.getType();
                        // 按照属性类型去找
                        bean = getBean(type);
                        if (bean == null) {
                            // 根据接口类型寻找
                            bean = getBeanByInterface(type);
                            if (bean == null)
                                throw new RuntimeException("No bean named " + clazz + " available.");
                        }
                    }

                    // 需要依赖注入的对象找到了
                    try {
                        declaredField.setAccessible(true);
                        Object o = containerMap.get(clazz);
                        declaredField.set(o, bean);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * 初始化配置类 @EConfiguration 中的 @EBean 注解标记的方法
     * 1. 这里默认所有的 @EBean 只能在 @EConfiguration 中被注册
     * 2. 默认 @EConfiguration 不能是接口或抽象类以及接口的实现，必须是一个具体的类
     */
    private void doInitConfigurationBeans() {
        Set<Map.Entry<Class<?>, Object>> entries = new HashSet<>(containerMap.entrySet());
        for (Map.Entry<Class<?>, Object> entry : entries) {
            Class<?> configClass = entry.getKey();
            if (!configClass.isAnnotationPresent(EConfiguration.class)) continue;
            // 找到所有的 @EConfiguration 注解标记的类
            Object configInstance = entry.getValue();
            // 拿到所有的方法
            Method[] methods = configClass.getDeclaredMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(EBean.class)) continue;
                // 拿到所有的 @EBean 注解标记的方法
                method.setAccessible(true);
                // 注入 @EBean 依赖
                Object bean = invokeEBeanMethod(configInstance, method);
                if (bean == null) {
                    throw new RuntimeException("@EBean method returned null: " + configClass.getName() + "#" + method.getName());
                }
                // 注册 @EBean 到容器
                EBean eBean = method.getAnnotation(EBean.class);
                String beanName = (eBean.value() == null || eBean.value().isEmpty()) ? method.getName() : eBean.value();
                registerBean(bean, beanName);
            }
        }
    }

    
    /**
     * 调用 @EBean 注解标记的方法
     * @param configInstance 配置类实例
     * @param method 被 @EBean 注解标记的方法
     * @return 方法执行结果
     */
    private Object invokeEBeanMethod(Object configInstance, Method method) {
        try {
            // 通过反射，那么这个函数的参数
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                // 直接通过类型去IOC容器里面找
                Object dependency = getBean(parameterType);
                // 如果没有找到，那么就按照接口类型去找
                if (dependency == null) dependency = getBeanByInterface(parameterType);
                // 如果还是没有找到，那么就报错
                if (dependency == null) {
                    throw new RuntimeException("No bean of type " + parameterType.getName() + " available for @EBean method " + method.getName());
                }
                // 把依赖注入到参数数组里面
                args[i] = dependency;
            }
            // 注入 @EBean 依赖
            return method.invoke(configInstance, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke @EBean method: " + method.getName(), e);
        }
    }

    /**
     * 注册 @EBean 到容器
     * @param bean
     * @param beanName
     */
    private void registerBean(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        // 检查是否已经存在相同类型的 bean
        if (containerMap.containsKey(beanClass)) {
            throw new RuntimeException("IOC container has already exist class bean: " + beanClass.getName());
        }
        containerMap.put(beanClass, bean);

        for (Class<?> anInterface : beanClass.getInterfaces()) {
            // 注册接口到容器
            List<Object> objects = interfaceContainerMap.get(anInterface);
            if (objects == null) {
                List<Object> objs = new ArrayList<>();
                objs.add(bean);
                interfaceContainerMap.put(anInterface, objs);
            } else {
                objects.add(bean);
            }
        }

        if (beanName != null && !beanName.isEmpty()) {
            if (!iocNameContainerMap.containsKey(beanName)) {
                iocNameContainerMap.put(beanName, bean);
            } else {
                throw new RuntimeException("IOC container has already exist: " + beanName);
            }
        }
    }

    private Object getBeanByInterface(Class<?> clazz) {
        if (interfaceContainerMap.containsKey(clazz)) {
            List<Object> objects = interfaceContainerMap.get(clazz);
            if (objects == null)
                return null;
            if (objects.size() > 1) {
                throw new RuntimeException("No qualifying bean of type " + clazz.getName() + " available: expected single matching bean but found " + objects.size()+ ":" + objects);
            }
            return objects.get(0);
        }
        return null;
    }

    public Object getBean(String objectName) {
        if (iocNameContainerMap.containsKey(objectName)) return iocNameContainerMap.get(objectName);
        return null;
    }

    public Object getBean(Class<?> clazz) {
        // 如果通过类能直接找到那就ok
        if (containerMap.containsKey(clazz)) return containerMap.get(clazz);
        return null;
    }


    /**
     * 扫描出所有被注解标记的类
     *
     * @return
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

    /**
     * 反射给扫描出来的类生成对象
     * @param annClasses 扫描出来的类
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void classObjectGeneration(Set<Class<?>> annClasses) throws InstantiationException, IllegalAccessException {
        for (Class<?> clazz : annClasses) {
            // 通过反射创建对象
            Object o = clazz.newInstance(); // 实例
            // 如果是接口 获得这个类实现的接口
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                // 把接口的实现类存储到容器中
                List<Object> objects = interfaceContainerMap.get(anInterface);
                if (objects == null) {
                    List<Object> objs = new ArrayList<>();
                    objs.add(o);
                    interfaceContainerMap.put(anInterface, objs);
                } else {
                    objects.add(o);
                }
            }
            // 把扫描的对象存储到ioc容器中
            containerMap.put(clazz, o);
            // 解析注解上的value值 如果为空 则使用类名作为对象名（bean的名字）
            String objectName = resolveStereotypeObjectName(clazz);
            if (objectName == null) continue;

            if (!iocNameContainerMap.containsKey(objectName))
                iocNameContainerMap.put(objectName, o);
            else
                throw new RuntimeException("IOC container has already exist: " + objectName);
        }
    }

    private String resolveStereotypeObjectName(Class<?> clazz) {
        EController eController = clazz.getAnnotation(EController.class);
        if (eController != null) {
            return (eController.value() == null || eController.value().isEmpty()) ? getObjectName(clazz) : eController.value();
        }

        EComponent eComponent = clazz.getAnnotation(EComponent.class);
        if (eComponent != null) {
            return (eComponent.value() == null || eComponent.value().isEmpty()) ? getObjectName(clazz) : eComponent.value();
        }

        EService eService = clazz.getAnnotation(EService.class);
        if (eService != null) {
            return (eService.value() == null || eService.value().isEmpty()) ? getObjectName(clazz) : eService.value();
        }

        EConfiguration eConfiguration = clazz.getAnnotation(EConfiguration.class);
        if (eConfiguration != null) {
            return (eConfiguration.value() == null || eConfiguration.value().isEmpty()) ? getObjectName(clazz) : eConfiguration.value();
        }

        for (Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (!annotationType.isAnnotationPresent(EComponent.class)) continue;
            try {
                Method valueMethod = annotationType.getMethod("value");
                Object value = valueMethod.invoke(annotation);
                if (!(value instanceof String)) return getObjectName(clazz);
                String valueStr = (String) value;
                return valueStr.isEmpty() ? getObjectName(clazz) : valueStr;
            } catch (NoSuchMethodException e) {
                return getObjectName(clazz);
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve bean name for " + clazz.getName(), e);
            }
        }

        return null;
    }


    private static String getObjectName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        return String.valueOf(simpleName.charAt(0)).toLowerCase() + simpleName.substring(1);
    }


}
