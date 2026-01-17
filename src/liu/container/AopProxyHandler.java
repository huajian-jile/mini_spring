package liu.container;

import liu.annotation.spring.aop.ExecutionTime;
import liu.annotation.spring.aop.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AOP 代理处理器
 * 处理 @Log 和 @ExecutionTime 注解的拦截逻辑
 */
public class AopProxyHandler implements InvocationHandler {
    
    private final Object target; // 目标对象
    private final Method aspectMethod; // 切面方法（可选）
    private final Object aspectInstance; // 切面实例（可选）
    
    public AopProxyHandler(Object target, Method aspectMethod, Object aspectInstance) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }
        this.target = target;
        this.aspectMethod = aspectMethod;
        this.aspectInstance = aspectInstance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 获取方法信息
        String methodName = method.getName();
        Class<?> declaringClass = method.getDeclaringClass();

        // 2. 处理 Object 类的基本方法
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
            // 其他 Object 方法直接执行
            return method.invoke(target, args);
        }

        // 3. 获取目标对象的实际方法（从实现类获取，而不是接口）
        Method targetMethod = null;
        try {
            targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            // 如果找不到方法，直接执行原方法
            return method.invoke(target, args);
        }

        // 4. 从目标方法上获取注解（重要：从实现类的方法获取）
        Log logAnnotation = targetMethod.getAnnotation(Log.class);
        ExecutionTime timeAnnotation = targetMethod.getAnnotation(ExecutionTime.class);

        // 5. 如果没有注解且没有切面，直接执行并返回
        if (logAnnotation == null && timeAnnotation == null && aspectMethod == null) {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        // 6. 执行前置逻辑
        long startTime = System.currentTimeMillis();

        if (aspectMethod != null) {
            System.out.println("🔍 [切面] 拦截方法: " + target.getClass().getSimpleName() + "." + methodName);
        }

        if (logAnnotation != null) {
            handleLogBefore(targetMethod, args, logAnnotation);
        }

        if (timeAnnotation != null) {
            handleExecutionTimeBefore(targetMethod, timeAnnotation);
        }

        // 7. 执行目标方法
        Object result = null;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            // 捕获业务异常
            Throwable cause = e.getCause();

            // 记录异常日志
            if (logAnnotation != null) {
                System.out.println("❌ [异常] 方法执行失败: " + 
                    (cause != null ? cause.getMessage() : "未知异常"));
            }

            // 重新抛出业务异常
            throw (cause != null ? cause : e);
        }

        // 8. 执行后置逻辑
        long duration = System.currentTimeMillis() - startTime;

        if (logAnnotation != null) {
            handleLogAfter(targetMethod, result, logAnnotation);
        }

        if (timeAnnotation != null) {
            handleExecutionTimeAfter(targetMethod, duration, timeAnnotation);
        }

        if (aspectMethod != null) {
            System.out.println("🔍 [切面] 方法执行完成，耗时: " + duration + "ms");
        }

        // 9. 返回结果（必须返回，避免 null 导致拆箱异常）
        return result;
    }
    
    /**
     * 处理日志注解的前置逻辑
     */
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
    
    /**
     * 处理日志注解的后置逻辑
     */
    private void handleLogAfter(Method method, Object result, Log logAnnotation) {
        if (logAnnotation.printResult()) {
            System.out.println("📝 [返回] " + (result != null ? result : "null"));
        }
        System.out.println("📝 ============ 日志结束 ============");
    }
    
    /**
     * 处理执行时间注解的前置逻辑
     */
    private void handleExecutionTimeBefore(Method method, ExecutionTime timeAnnotation) {
        String description = timeAnnotation.value().isEmpty() ? 
            method.getName() : timeAnnotation.value();
        System.out.println("⏱️  [开始执行] " + description);
    }
    
    /**
     * 处理执行时间注解的后置逻辑
     */
    private void handleExecutionTimeAfter(Method method, long duration, ExecutionTime timeAnnotation) {
        String description = timeAnnotation.value().isEmpty() ? 
            method.getName() : timeAnnotation.value();
        
        // 如果设置了阈值，只有超过阈值才打印
        if (timeAnnotation.threshold() > 0 && duration < timeAnnotation.threshold()) {
            return;
        }
        
        // 根据耗时选择不同的显示格式
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

