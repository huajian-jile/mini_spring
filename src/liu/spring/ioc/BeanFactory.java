package liu.spring.ioc;

/**
 * Bean 工厂接口（类似 Spring 的 BeanFactory）。
 * 负责 Bean 的获取，具体创建与生命周期由实现类管理。
 */
public interface BeanFactory {

    Object getBean(String name);
    <T> T getBean(Class<T> requiredType);
    <T> T getBean(String name, Class<T> requiredType);
    boolean containsBean(String name);
    Object getBeanByType(Class<?> requiredType);
}
