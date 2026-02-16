package liu.spring.ioc;

import java.util.Set;

/**
 * Bean 定义注册中心（类似 Spring 的 BeanDefinitionRegistry）。
 */
public interface BeanDefinitionRegistry {

    void registerBeanDefinition(String beanName, BeanDefinition definition);
    BeanDefinition getBeanDefinition(String beanName);
    boolean containsBeanDefinition(String beanName);
    Set<String> getBeanDefinitionNames();
}
