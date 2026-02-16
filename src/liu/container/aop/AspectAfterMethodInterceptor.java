package liu.container.aop;

import java.lang.reflect.Method;

/**
 * 对 @After 切面方法的拦截器：先 proceed，再执行切面方法（finally 型）。
 */
public class AspectAfterMethodInterceptor implements MethodInterceptor, OrderedAdapter {

    private final Object aspectInstance;
    private final Method adviceMethod;
    private final int order;

    public AspectAfterMethodInterceptor(Object aspectInstance, Method adviceMethod, int order) {
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
        } finally {
            AspectInvocationHelper.invokeAdvice(aspectInstance, adviceMethod,
                    invocation.getTarget(), invocation.getMethod(), invocation.getArguments(), null, null);
        }
    }
}
