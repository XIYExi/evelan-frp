package net.evelan.frp.bootstrap.core;

import net.evelan.frp.bootstrap.annotation.EComponent;
import net.evelan.frp.bootstrap.annotation.EController;
import net.evelan.frp.bootstrap.utils.ReflectionUtil;

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
        initialize();
    }


    /**
     * 初始化application上下文容器
     */
    private void initialize() {
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
