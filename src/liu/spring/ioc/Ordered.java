package liu.spring.ioc;

/**
 * 顺序接口（与 Spring 一致）。BeanPostProcessor 等可实现此接口以指定执行顺序，数字越小越先执行。
 */
public interface Ordered {
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;
    int getOrder();
}
