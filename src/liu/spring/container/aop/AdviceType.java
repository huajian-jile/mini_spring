package liu.spring.container.aop;

/**
 * 通知类型（与 Spring 的 Advice 类型对应）。
 */
public enum AdviceType {
    BEFORE,
    AFTER,
    AFTER_RETURNING,
    AFTER_THROWING,
    AROUND
}
