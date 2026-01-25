package liu.aspect.logaspect;

import liu.annotation.spring.aop.Advice;
import liu.annotation.spring.aop.Around;
import liu.annotation.spring.aop.Aspect;
import liu.annotation.spring.ioc.Component;

import java.lang.reflect.Method;

/**
 * 日志切面
 * 实现 Advice 接口，提供具体的切面逻辑
 */
@Aspect
@Component
public class LogAspect implements Advice {
    
    /**
     * 切点配置：拦截 service 包下所有类
     */
    @Around("liu.service.*")
    public Object pointcut() {
        // 这个方法只用于标记切点，不会被实际调用
        return null;
    }
    
    /**
     * 前置通知：在方法执行前记录日志
     */
    @Override
    public void before(Object target, Method method, Object[] args) {
        System.out.println("🔍 [LogAspect] 前置通知");
        System.out.println("    ├─ 目标类: " + target.getClass().getSimpleName());
        System.out.println("    ├─ 方法名: " + method.getName());
        System.out.println("    └─ 参数数量: " + (args != null ? args.length : 0));
    }
    
    /**
     * 后置通知：在方法执行后记录日志
     */
    @Override
    public void after(Object target, Method method, Object[] args, Object result) {
        System.out.println("🔍 [LogAspect] 后置通知");
        System.out.println("    └─ 返回值类型: " + (result != null ? result.getClass().getSimpleName() : "void"));
    }
    
    /**
     * 异常通知：在方法抛出异常时记录
     */
    @Override
    public void afterThrowing(Object target, Method method, Object[] args, Throwable throwable) {
        System.out.println("🔍 [LogAspect] 异常通知");
        System.out.println("    └─ 异常信息: " + throwable.getMessage());
    }
    
    /**
     * 环绕通知：完全控制方法执行流程
     * 如果需要自定义完整流程，可以重写这个方法
     */
    @Override
    public Object around(Object target, Method method, Object[] args) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 调用前置通知
        before(target, method, args);
        
        Object result = null;
        try {
            // 执行目标方法
            result = method.invoke(target, args);
            
            // 调用后置通知
            after(target, method, args, result);
        } catch (Throwable e) {
            // 调用异常通知
            afterThrowing(target, method, args, e);
            throw e;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("🔍 [LogAspect] 方法执行耗时: " + duration + "ms");
        
        return result;
    }
}
