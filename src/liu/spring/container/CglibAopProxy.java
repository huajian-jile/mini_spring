package liu.spring.container;

import liu.spring.container.aop.AopAdvisorRegistry;
import liu.spring.container.aop.AopContext;
import liu.spring.container.aop.MethodInterceptor;
import liu.spring.container.aop.ReflectiveMethodInvocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 基于 CGLIB 的 AOP 代理（无接口时使用，与 Spring 的 CglibAopProxy 对应）。
 * 依赖 cglib，需在 pom 中引入：cglib:cglib。
 */
public class CglibAopProxy implements net.sf.cglib.proxy.MethodInterceptor {

    private final Object target;
    private final AopAdvisorRegistry advisorRegistry;

    public CglibAopProxy(Object target, AopAdvisorRegistry advisorRegistry) {
        if (target == null) throw new IllegalArgumentException("Target object cannot be null");
        this.target = target;
        this.advisorRegistry = advisorRegistry;
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, net.sf.cglib.proxy.MethodProxy methodProxy) throws Throwable {
        try {
            AopContext.setCurrentProxy(proxy);
            return doIntercept(proxy, method, args);
        } finally {
            AopContext.clearCurrentProxy();
        }
    }

    private Object doIntercept(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }
        Method targetMethod;
        try {
            targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return method.invoke(target, args);
        }
        List<MethodInterceptor> chain = advisorRegistry.getInterceptors(target, targetMethod);
        if (chain.isEmpty()) {
            return method.invoke(target, args);
        }
        ReflectiveMethodInvocation invocation = new ReflectiveMethodInvocation(target, targetMethod, args, chain);
        try {
            return invocation.proceed();
        } catch (InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }
    }

    /**
     * 为无接口的 Bean 创建 CGLIB 代理；若 CGLIB 不可用则返回 null。
     */
    @SuppressWarnings("unchecked")
    public static Object createProxy(Object target, AopAdvisorRegistry advisorRegistry) {
        Class<?> targetClass = target.getClass();
        if (targetClass.isInterface()) return null;
        net.sf.cglib.proxy.Enhancer enhancer = new net.sf.cglib.proxy.Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(new CglibAopProxy(target, advisorRegistry));
        enhancer.setClassLoader(targetClass.getClassLoader());
        return enhancer.create();
    }
}
