package liu.container;

import liu.aspect.Advice;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AopProxyHandler implements InvocationHandler {

    // 1. 目标对象
    private final Object target;

    // 2. 通知对象 (核心改动：不再传 Method 和 AspectInstance，而是直接传 Advice)
    private final Advice advice;

    public AopProxyHandler(Object target, Advice advice) {
        this.target = target;
        this.advice = advice;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // Object 基础方法放行
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }

        // 获取真实方法 (用于反射调用)
        Method targetMethod = null;
        try {
            targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return method.invoke(target, args);
        }

        // --- 核心 AOP 流程 ---

        // 1. 前置通知
        try {
            advice.before(targetMethod, args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. 执行目标方法 (核心业务逻辑)
        Object result = null;
        try {
            result = method.invoke(target, args);
            // 3. 返回通知
            advice.after(targetMethod, result);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            // 4. 异常通知
            advice.afterThrowing(targetMethod, cause);
            throw cause;
        } catch (Exception e) {
            advice.afterThrowing(targetMethod, e);
            throw e;
        }

        return result;
    }
}