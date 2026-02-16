package liu.spring.container.aop;

import liu.spring.annotation.spring.aop.Advice;

import java.lang.reflect.Method;

/**
 * 将 @Aspect 的 Advice 适配为 MethodInterceptor，便于纳入统一拦截器链。
 * 通过 before -> invocation.proceed() -> after 保证拦截器链继续执行，避免直接调用
 * advice.around(..) 导致链断裂（around 内部 method.invoke 会跳过后续拦截器）。
 */
public class AspectMethodInterceptor implements MethodInterceptor, OrderedAdapter {

    private final Advice advice;
    private final int order;

    public AspectMethodInterceptor(Advice advice) {
        this(advice, Ordered.LOWEST_PRECEDENCE);
    }

    public AspectMethodInterceptor(Advice advice, int order) {
        this.advice = advice;
        this.order = order;
    }

    @Override
    public int getOrder() { return order; }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (advice == null) {
            return invocation.proceed();
        }
        Object target = invocation.getTarget();
        Method method = invocation.getMethod();
        Object[] args = invocation.getArguments();
        advice.before(target, method, args);
        Object result;
        try {
            result = invocation.proceed();
            advice.after(target, method, args, result);
            return result;
        } catch (Throwable e) {
            advice.afterThrowing(target, method, args, e);
            throw e;
        }
    }
}
