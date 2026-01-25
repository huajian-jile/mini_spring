package liu.container;

import liu.annotation.spring.aop.Advice;
import liu.annotation.spring.aop.ExecutionTime;
import liu.annotation.spring.aop.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AOP 代理处理器（重构版）
 * 使用 Advice 接口实现解耦
 */
public class AopProxyHandler implements InvocationHandler {
    
    private final Object target;        // 目标对象
    private final Advice advice;        // 通知（切面逻辑）
    
    public AopProxyHandler(Object target, Advice advice) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }
        this.target = target;
        this.advice = advice;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 处理 Object 类的基本方法
        String methodName = method.getName();
        Class<?> declaringClass = method.getDeclaringClass();

        if (declaringClass == Object.class) {
            if ("toString".equals(methodName)) {
                return target.toString();
            }
            if ("hashCode".equals(methodName)) {
                return target.hashCode();
            }
            if ("equals".equals(methodName)) {
                return target.equals(args != null && args.length > 0 ? args[0] : null);
            }
            if ("getClass".equals(methodName)) {
                return target.getClass();
            }
            return method.invoke(target, args);
        }

        // 2. 获取目标方法（从实现类获取，以便获取注解）
        Method targetMethod = null;
        try {
            targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return method.invoke(target, args);
        }

        // 3. 检查是否有 @Log 或 @ExecutionTime 注解
        Log logAnnotation = targetMethod.getAnnotation(Log.class);
        ExecutionTime timeAnnotation = targetMethod.getAnnotation(ExecutionTime.class);
        
        // 4. 如果有注解，使用内置的注解处理逻辑
        if (logAnnotation != null || timeAnnotation != null) {
            return handleAnnotations(targetMethod, args, logAnnotation, timeAnnotation);
        }

        // 5. 否则，使用切面的 Advice 逻辑
        if (advice != null) {
            return advice.around(target, method, args);
        }

        // 6. 如果都没有，直接执行原方法
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
    
    /**
     * 处理注解逻辑（@Log 和 @ExecutionTime）
     */
    private Object handleAnnotations(Method targetMethod, Object[] args, 
                                     Log logAnnotation, ExecutionTime timeAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 日志前置
        if (logAnnotation != null) {
            handleLogBefore(targetMethod, args, logAnnotation);
        }

        // 执行时间前置
        if (timeAnnotation != null) {
            handleExecutionTimeBefore(targetMethod, timeAnnotation);
        }

        // 执行目标方法
        Object result = null;
        try {
            result = targetMethod.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (logAnnotation != null) {
                System.out.println("❌ [异常] 方法执行失败: " + 
                    (cause != null ? cause.getMessage() : "未知异常"));
            }
            throw (cause != null ? cause : e);
        }

        // 执行后置逻辑
        long duration = System.currentTimeMillis() - startTime;

        if (logAnnotation != null) {
            handleLogAfter(targetMethod, result, logAnnotation);
        }

        if (timeAnnotation != null) {
            handleExecutionTimeAfter(targetMethod, duration, timeAnnotation);
        }

        return result;
    }
    
    private void handleLogBefore(Method method, Object[] args, Log logAnnotation) {
        String description = logAnnotation.value().isEmpty() ? 
            method.getName() : logAnnotation.value();
        
        System.out.println("📝 ============ 日志开始 ============");
        System.out.println("📝 [方法] " + target.getClass().getSimpleName() + "." + method.getName());
        System.out.println("📝 [描述] " + description);
        
        if (logAnnotation.printArgs() && args != null && args.length > 0) {
            System.out.println("📝 [参数] " + Arrays.toString(args));
        }
    }
    
    private void handleLogAfter(Method method, Object result, Log logAnnotation) {
        if (logAnnotation.printResult()) {
            System.out.println("📝 [返回] " + (result != null ? result : "null"));
        }
        System.out.println("📝 ============ 日志结束 ============");
    }
    
    private void handleExecutionTimeBefore(Method method, ExecutionTime timeAnnotation) {
        String description = timeAnnotation.value().isEmpty() ? 
            method.getName() : timeAnnotation.value();
        System.out.println("⏱️  [开始执行] " + description);
    }
    
    private void handleExecutionTimeAfter(Method method, long duration, ExecutionTime timeAnnotation) {
        String description = timeAnnotation.value().isEmpty() ? 
            method.getName() : timeAnnotation.value();
        
        if (timeAnnotation.threshold() > 0 && duration < timeAnnotation.threshold()) {
            return;
        }
        
        if (duration > 1000) {
            System.out.println("⏱️  [执行完成] " + description + " - 耗时: " + 
                String.format("%.2f", duration / 1000.0) + "s ⚠️");
        } else if (duration > 500) {
            System.out.println("⏱️  [执行完成] " + description + " - 耗时: " + duration + "ms");
        } else {
            System.out.println("⏱️  [执行完成] " + description + " - 耗时: " + duration + "ms ✅");
        }
    }
}
