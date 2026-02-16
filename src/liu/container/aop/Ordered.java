package liu.container.aop;

/**
 * 顺序常量（与 Spring 的 Ordered 一致）。
 */
public interface Ordered {
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;
}
