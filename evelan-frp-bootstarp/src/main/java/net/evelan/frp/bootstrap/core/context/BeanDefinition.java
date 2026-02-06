package net.evelan.frp.bootstrap.core.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Bean定义
 */
public class BeanDefinition implements Comparable<BeanDefinition> {
    // 全局唯一的Bean Name
    String name;
    // Bean的声明类型
    // 并不是实际类型！如接口类，声明的是TestService，但是实现类确实TestServiceImpl，这里存的是TestService！
    Class<?> beanClass;
    // 实例！
    Object instance = null;
    // 构造方法
    Constructor<?> constructor;
    // 工厂方法名称
    String factoryName;
    // 工厂方法
    Method factoryMethod;
    // 顺序，被@Order标记
    int order;
    // 是否被@Primary标记
    boolean primary;

    // init/destroy方法名称
    String initMethodName;
    String destroyMethodName;

    // init/destroy方法
    Method initMethod;
    Method destroyMethod;

    public BeanDefinition(
            String name,
            Class<?> beanClass,
            Constructor<?> constructor,
            int order,
            boolean primary,
            String initMethodName,
            String destroyMethodName,
            Method initMethod,
            Method destroyMethod
    ) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = constructor;
        this.factoryName = null;
        this.factoryMethod = null;
        this.order = order;
        this.primary = primary;
        constructor.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    public BeanDefinition(
            String name,
            Class<?> beanClass,
            String factoryName,
            Method factoryMethod,
            int order,
            boolean primary,
            String initMethodName,
            String destroyMethodName,
            Method initMethod,
            Method destroyMethod
    ) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = null;
        this.factoryName = factoryName;
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        factoryMethod.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }



    private void setInitAndDestroyMethod(
            String initMethodName,
            String destroyMethodName,
            Method initMethod,
            Method destroyMethod
    ) {
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        if (initMethod != null) {
            initMethod.setAccessible(true);
        }
        if (destroyMethod != null) {
            destroyMethod.setAccessible(true);
        }
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }


    public Object getRequiredInstance() {
        if (this.instance == null) {
            throw new RuntimeException(
                    String.format(
                            "Instance of bean with name '%s' and type '%s' is not instantiated during current stage.",
                            this.getName(),
                            this.getBeanClass().getName()
                    )
            );
        }
        return this.instance;
    }

    public void setInstance(Object instance) {
        Objects.requireNonNull(instance, "Bean instance is null.");
        if (!this.beanClass.isAssignableFrom(instance.getClass())) {
            throw new RuntimeException(
                    String.format(
                            "Instance '%s' of Bean '%s' is not the expected type: %s",
                            instance,
                            instance.getClass().getName(),
                            this.beanClass.getName()
                    )
            );
        }
        this.instance = instance;
    }

    public boolean isPrimary() {
        return this.primary;
    }

    @Override
    public String toString() {
        return "BeanDefinition [name=" + name + ", beanClass=" + beanClass.getName() + ", factory=" + getCreateDetail() + ", init-method="
                + (initMethod == null ? "null" : initMethod.getName()) + ", destroy-method=" + (destroyMethod == null ? "null" : destroyMethod.getName())
                + ", primary=" + primary + ", instance=" + instance + "]";
    }

    String getCreateDetail() {
        if (this.factoryMethod != null) {
            String params = String.join(", ", Arrays.stream(this.factoryMethod.getParameterTypes()).map(Class::getSimpleName).toArray(String[]::new));
            return this.factoryMethod.getDeclaringClass().getSimpleName() + "." + this.factoryMethod.getName() + "(" + params + ")";
        }
        return null;
    }


    /**
     * Bean定义的比较器，先比较顺序，再比较Bean Name
     * @param def 需要比较的Bean容器
     * @return 0表示相同，正数表示this大于def，负数表示this小于def
     */
    @Override
    public int compareTo(BeanDefinition def) {
        int cmp = Integer.compare(this.order, def.order);
        if (cmp != 0) {
            return cmp;
        }
        return this.name.compareTo(def.name);
    }


    public Constructor<?> getConstructor() {
        return this.constructor;
    }

    public String getFactoryName() {
        return this.factoryName;
    }

    public Method getFactoryMethod() {
        return this.factoryMethod;
    }

    public Method getInitMethod() {
        return this.initMethod;
    }

    public Method getDestroyMethod() {
        return this.destroyMethod;
    }

    public String getInitMethodName() {
        return this.initMethodName;
    }

    public String getDestroyMethodName() {
        return this.destroyMethodName;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    public Object getInstance() {
        return this.instance;
    }

    public int getOrder() {
        return this.order;
    }

}
