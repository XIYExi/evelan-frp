package net.evelan.frp.bootstrap.core;

import net.evelan.frp.bootstrap.annotation.EComponent;
import net.evelan.frp.bootstrap.annotation.EController;
import net.evelan.frp.bootstrap.utils.ReflectionUtil;

import java.util.List;
import java.util.Set;

public class AssembleApplicationContext {

    /**
     * 创建一个应用上下文
     * @param configFilePath yaml配置文件路径
     */
    public AssembleApplicationContext(String configFilePath) {
        initialize();
    }


    /**
     * 初始化application上下文容器
     */
    private void initialize() {
        // 扫描出来被注解标记的所有类
        Set<Class<?>> annClasses = collectBeanObject();
    }

    private Set<Class<?>> collectBeanObject() {
        // 获取所有被 @EBean @Component @EController 注解标记的类
        Set<Class<?>> annotatedClasses = ReflectionUtil.findAnnotatedClasses(
                "net.evelan.frp",
                List.of(EController.class, EComponent.class),
                true
        );
        annotatedClasses.forEach(clazz -> {
            System.out.println(clazz.getName());
        });
        return annotatedClasses;
    }



}
