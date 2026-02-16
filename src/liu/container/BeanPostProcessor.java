package liu.container;

/**
 * Bean 后置处理器（类似 Spring 的 BeanPostProcessor）。
 * 在 Bean 初始化前后插入逻辑，是实现 AOP、注解处理等扩展的核心机制。
 * 新增功能时只需实现此接口并注册到容器即可，无需修改核心代码。
 */
public interface BeanPostProcessor {

    /**
     * 在 Bean 初始化之前调用（属性注入之后）
     *
     * @param bean     当前 Bean 实例
     * @param beanName Bean 名称
     * @return 可返回包装后的 Bean，或原 Bean
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 在 Bean 初始化之后调用（如 AOP 代理在此阶段创建）
     *
     * @param bean     当前 Bean 实例
     * @param beanName Bean 名称
     * @return 可返回代理对象替换原 Bean（如 AOP），或原 Bean
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
