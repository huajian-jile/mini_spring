package liu.spring.ioc;

/**
 * Bean 后置处理器（类似 Spring 的 BeanPostProcessor）。
 * 在 Bean 初始化前后插入逻辑，是实现 AOP、注解处理等扩展的核心机制。
 */
public interface BeanPostProcessor {

    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
