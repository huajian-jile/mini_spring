package liu.spring.container.aop;

import java.lang.reflect.Method;

/**
 * 对 @Before 切面方法的拦截器：先执行切面方法，再 proceed。
 */
public class AspectBeforeMethodInterceptor implements MethodInterceptor, OrderedAdapter {

    private final Object aspectInstance;
    private final Method adviceMethod;
    private final int order;

    public AspectBeforeMethodInterceptor(Object aspectInstance, Method adviceMethod, int order) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.order = order;
    }

    @Override
    public int getOrder() { return order; }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        AspectInvocationHelper.invokeAdvice(aspectInstance, adviceMethod,
                invocation.getTarget(), invocation.getMethod(), invocation.getArguments(), null, null);
        return invocation.proceed();
    }
}
