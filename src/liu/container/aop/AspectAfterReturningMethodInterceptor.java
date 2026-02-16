package liu.container.aop;

import java.lang.reflect.Method;

/**
 * 对 @AfterReturning 切面方法的拦截器：proceed 后若正常返回则执行切面方法。
 */
public class AspectAfterReturningMethodInterceptor implements MethodInterceptor, OrderedAdapter {

    private final Object aspectInstance;
    private final Method adviceMethod;
    private final int order;

    public AspectAfterReturningMethodInterceptor(Object aspectInstance, Method adviceMethod, int order) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.order = order;
    }

    @Override
    public int getOrder() { return order; }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = invocation.proceed();
        AspectInvocationHelper.invokeAdvice(aspectInstance, adviceMethod,
                invocation.getTarget(), invocation.getMethod(), invocation.getArguments(), result, null);
        return result;
    }
}
