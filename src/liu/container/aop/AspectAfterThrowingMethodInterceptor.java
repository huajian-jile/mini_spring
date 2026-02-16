package liu.container.aop;

import java.lang.reflect.Method;

/**
 * 对 @AfterThrowing 切面方法的拦截器：proceed 若抛异常则执行切面方法后继续抛出。
 */
public class AspectAfterThrowingMethodInterceptor implements MethodInterceptor, OrderedAdapter {

    private final Object aspectInstance;
    private final Method adviceMethod;
    private final int order;

    public AspectAfterThrowingMethodInterceptor(Object aspectInstance, Method adviceMethod, int order) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.order = order;
    }

    @Override
    public int getOrder() { return order; }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (Throwable ex) {
            AspectInvocationHelper.invokeAdvice(aspectInstance, adviceMethod,
                    invocation.getTarget(), invocation.getMethod(), invocation.getArguments(), null, ex);
            throw ex;
        }
    }
}
