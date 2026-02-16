package liu.container;

/**
 * Bean 工厂接口（类似 Spring 的 BeanFactory）。
 * 负责 Bean 的获取，具体创建与生命周期由实现类管理。
 */
public interface BeanFactory {

    /**
     * 根据名称获取 Bean
     */
    Object getBean(String name);

    /**
     * 根据类型获取 Bean
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * 根据名称和类型获取 Bean
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 是否包含指定名称的 Bean
     */
    boolean containsBean(String name);

    /**
     * 根据类型查找 Bean（可能为代理，需匹配接口或原始类型）
     */
    Object getBeanByType(Class<?> requiredType);
}
