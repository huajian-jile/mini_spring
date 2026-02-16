package liu.spring.container;

import liu.spring.container.aop.AopAdvisorRegistry;
import liu.spring.container.aop.MethodInterceptor;
import liu.spring.container.aop.ReflectiveMethodInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * AOP 代理处理器（可扩展版）。
 * 通过 AopAdvisorRegistry 获取拦截器链，按链执行，不再写死 @Log / @ExecutionTime / Advice。
 */
public class AopProxyHandler implements InvocationHandler {

    private final Object target;
    private final AopAdvisorRegistry advisorRegistry;

    public AopProxyHandler(Object target, AopAdvisorRegistry advisorRegistry) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }
        this.target = target;
        this.advisorRegistry = advisorRegistry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass == Object.class) {
            switch (method.getName()) {
                case "toString":
                    return target.toString();
                case "hashCode":
                    return target.hashCode();
                case "equals":
                    return target.equals(args != null && args.length > 0 ? args[0] : null);
                case "getClass":
                    return target.getClass();
                default:
                    return method.invoke(target, args);
            }
        }

        Method targetMethod;
        try {
            targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return method.invoke(target, args);
        }

        List<MethodInterceptor> chain = advisorRegistry.getInterceptors(target, targetMethod);
        if (chain.isEmpty()) {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        ReflectiveMethodInvocation invocation = new ReflectiveMethodInvocation(target, method, args, chain);
        try {
            return invocation.proceed();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
