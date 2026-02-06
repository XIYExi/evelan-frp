package net.evelan.frp.bootstrap.core.core;

import net.evelan.frp.bootstrap.core.context.BeanDefinition;

import java.util.List;

public interface ConfigurableApplicationContext extends ApplicationContext{
    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    BeanDefinition findBeanDefinition(Class<?> type);

    BeanDefinition findBeanDefinition(String name);

    BeanDefinition findBeanDefinition(String name, Class<?> requiredType);

    Object createBeanAsEarlySingleton(BeanDefinition def);
}
