package liu.container.aop;

import java.lang.reflect.Method;

/**
 * 方法调用上下文（类似 Spring 的 MethodInvocation）。
 * 封装目标对象、方法、参数，以及 proceed() 驱动拦截器链。
 */
public interface MethodInvocation {

    Object getTarget();

    Method getMethod();

    Object[] getArguments();

    /**
     * 执行链中的下一个拦截器，或最终执行目标方法
     */
    Object proceed() throws Throwable;
}
