package net.evelan.frp.bootstrap.core;

import net.evelan.frp.bootstrap.annotation.EComponent;
import net.evelan.frp.bootstrap.annotation.EController;
import net.evelan.frp.bootstrap.annotation.EImport;
import net.evelan.frp.bootstrap.annotation.EPostConstruct;
import net.evelan.frp.bootstrap.utils.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AssembleApplicationContext {

    /**
     * IOC 容器
     * key: 被扫描对象实现的接口 value：接口实现类
     */
    private static final Map<Class<?>, List<Object>> interfaceContainerMap = new ConcurrentHashMap<>();
    /**
     * IOC 容器
     * key: 被扫描的class value: 对象
     */
    private static final Map<Class<?>, Object> containerMap = new ConcurrentHashMap<>();
    /**
     * IOC扫描的对象
     * key：对象名字 value：对象
     */
    private static Map<String, Object> iocNameContainerMap = new ConcurrentHashMap<>();

    /**
     * 创建一个应用上下文
     *
     * @param configFilePath yaml配置文件路径
     */
    public AssembleApplicationContext(String configFilePath) {
        doInitInstance();
        doDI();
        doInitMethods();
    }


    /**
     * 执行初始化方法
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
        String[] currentUtilClassAllClassnames = AssembleApplicationContext.class.getPackageName().split("\\.");
        // 获取所有被 @EBean @Component @EController 注解标记的类
        Set<Class<?>> annotatedClasses = ReflectionUtil.findAnnotatedClasses(
                currentUtilClassAllClassnames[0], // 从最顶部的包开始往下扫
                List.of(EController.class, EComponent.class),
                true
        );
        annotatedClasses.forEach(clazz -> {
            System.out.println(clazz.getName());
        });
        return annotatedClasses;
    }

    /**
     * 反射给扫描出来的类生成对象
     */
    private void classObjectGeneration(Set<Class<?>> annClasses) throws InstantiationException, IllegalAccessException {
        for (Class<?> clazz : annClasses) {
            // 通过反射创建对象
            Object o = clazz.newInstance();
            // 获得这个类实现的接口
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                // 把接口的实现类存储到容器中
                List<Object> objects = interfaceContainerMap.get(anInterface);
                if (objects == null) {
                    List<Object> objs = new ArrayList<>();
                    objs.add(o);
                    interfaceContainerMap.put(anInterface, objs);
                } else {
                    objects.add(0);
                }
            }
            // 把扫描的对象存储到ioc容器中
            containerMap.put(clazz, o);
            // 要么有@EController注解，要么有@EComponent注解
            EController eController = clazz.getAnnotation(EController.class);
            EComponent eComponent = clazz.getAnnotation(EComponent.class);
            if (eComponent != null || eController != null) {
                String value = null;
                if (eController != null) {
                    value = eController.value();
                } else if (eComponent != null){
                    value = eComponent.value();
                }
                // 对象的名字
                String objectName = value == null || value.isEmpty() ? getObjectName(clazz) : value;

                // 把对象存储起来
                if (!iocNameContainerMap.containsKey(objectName))
                    iocNameContainerMap.put(objectName, o);
                else
                    throw new RuntimeException("IOC container has already exist: " + objectName);
            }
        }
    }


    private static String getObjectName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        return String.valueOf(simpleName.charAt(0)).toLowerCase() + simpleName.substring(1);
    }


}
