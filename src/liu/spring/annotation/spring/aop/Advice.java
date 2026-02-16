package liu.spring.annotation.spring.aop;

import java.lang.reflect.Method;

/**
 * 通知接口
 * 定义切面的通用行为
 */
public interface Advice {
    
    /**
     * 前置通知：在目标方法执行之前调用
     * 
     * @param target 目标对象
     * @param method 目标方法
     * @param args 方法参数
     */
    default void before(Object target, Method method, Object[] args) {
        // 默认空实现
    }
    
    /**
     * 后置通知：在目标方法执行之后调用
     * 
     * @param target 目标对象
     * @param method 目标方法
     * @param args 方法参数
     * @param result 方法返回值
     */
    default void after(Object target, Method method, Object[] args, Object result) {
        // 默认空实现
    }
    
    /**
     * 异常通知：在目标方法抛出异常时调用
     * 
     * @param target 目标对象
     * @param method 目标方法
     * @param args 方法参数
     * @param throwable 异常对象
     */
    default void afterThrowing(Object target, Method method, Object[] args, Throwable throwable) {
        // 默认空实现
    }
    
    /**
     * 环绕通知：完全控制目标方法的执行
     * 
     * @param target 目标对象
     * @param method 目标方法
     * @param args 方法参数
     * @return 方法执行结果
     * @throws Throwable 可能抛出的异常
     */
    default Object around(Object target, Method method, Object[] args) throws Throwable {
        before(target, method, args);
        Object result = null;
        try {
            result = method.invoke(target, args);
            after(target, method, args, result);
        } catch (Throwable e) {
            afterThrowing(target, method, args, e);
            throw e;
        }
        return result;
    }
}

