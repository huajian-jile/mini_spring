package liu.spring.container.aop;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 基于反射的 MethodInvocation 实现，按顺序执行拦截器链，最后反射调用目标方法。
 */
public class ReflectiveMethodInvocation implements MethodInvocation {

    private final Object target;
    private final Method method;
    private final Object[] arguments;
    private final List<MethodInterceptor> interceptors;
    private int currentIndex = 0;

    public ReflectiveMethodInvocation(Object target, Method method, Object[] arguments,
                                      List<MethodInterceptor> interceptors) {
        this.target = target;
        this.method = method;
        this.arguments = arguments;
        this.interceptors = interceptors != null ? interceptors : new java.util.ArrayList<>();
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object proceed() throws Throwable {
        if (currentIndex < interceptors.size()) {
            MethodInterceptor interceptor = interceptors.get(currentIndex++);
            return interceptor.invoke(this);
        }
        Object[] args = arguments != null ? arguments : new Object[0];
        return method.invoke(target, args);
    }
}
