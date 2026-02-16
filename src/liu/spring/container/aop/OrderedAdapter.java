package liu.spring.container.aop;

/**
 * 带顺序的拦截器（用于链排序）。
 */
public interface OrderedAdapter {
    int getOrder();
}
