package liu.container;

import java.util.Set;

/**
 * Bean 定义注册中心（类似 Spring 的 BeanDefinitionRegistry）。
 * 支持注册、获取 BeanDefinition，便于扩展时动态添加 Bean 定义。
 */
public interface BeanDefinitionRegistry {

    /**
     * 注册 Bean 定义
     */
    void registerBeanDefinition(String beanName, BeanDefinition definition);

    /**
     * 获取 Bean 定义
     */
    BeanDefinition getBeanDefinition(String beanName);

    /**
     * 是否包含指定名称的 Bean 定义
     */
    boolean containsBeanDefinition(String beanName);

    /**
     * 获取所有已注册的 Bean 名称
     */
    Set<String> getBeanDefinitionNames();
}
