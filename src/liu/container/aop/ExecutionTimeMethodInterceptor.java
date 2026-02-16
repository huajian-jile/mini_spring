package liu.container.aop;

import liu.annotation.spring.aop.ExecutionTime;

import java.lang.reflect.Method;

/**
 * 对带 @ExecutionTime 的方法做耗时统计。通过 AopAdvisorRegistry 注册即可生效，无需改代理核心。
 */
public class ExecutionTimeMethodInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = getTargetMethod(invocation);
        ExecutionTime timeAnnotation = method == null ? null : method.getAnnotation(ExecutionTime.class);
        if (timeAnnotation == null) {
            return invocation.proceed();
        }

        String description = timeAnnotation.value().isEmpty() ? method.getName() : timeAnnotation.value();
        System.out.println("⏱️  [开始执行] " + description);
        long start = System.currentTimeMillis();

        Object result = invocation.proceed();

        long duration = System.currentTimeMillis() - start;
        if (timeAnnotation.threshold() > 0 && duration < timeAnnotation.threshold()) {
            return result;
        }
        if (duration > 1000) {
            System.out.println("⏱️  [执行完成] " + description + " - 耗时: " +
                    String.format("%.2f", duration / 1000.0) + "s ⚠️");
        } else if (duration > 500) {
            System.out.println("⏱️  [执行完成] " + description + " - 耗时: " + duration + "ms");
        } else {
            System.out.println("⏱️  [执行完成] " + description + " - 耗时: " + duration + "ms ✅");
        }
        return result;
    }

    private Method getTargetMethod(MethodInvocation invocation) {
        try {
            return invocation.getTarget().getClass().getMethod(
                    invocation.getMethod().getName(),
                    invocation.getMethod().getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
