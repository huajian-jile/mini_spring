package liu.spring.container.aop;

import liu.spring.annotation.spring.aop.Log;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 对带 @Log 的方法做日志环绕增强。通过 AopAdvisorRegistry 注册即可生效，无需改代理核心。
 */
public class LogMethodInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = getTargetMethod(invocation);
        Log logAnnotation = method == null ? null : method.getAnnotation(Log.class);
        if (logAnnotation == null) {
            return invocation.proceed();
        }

        before(method, invocation.getArguments(), logAnnotation);
        Object result;
        try {
            result = invocation.proceed();
        } catch (Throwable e) {
            System.out.println("❌ [异常] 方法执行失败: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            throw e;
        }
        after(method, result, logAnnotation);
        return result;
    }

    private void before(Method method, Object[] args, Log logAnnotation) {
        String description = logAnnotation.value().isEmpty() ? method.getName() : logAnnotation.value();
        System.out.println("📝 ============ 日志开始 ============");
        System.out.println("📝 [方法] " + method.getDeclaringClass().getSimpleName() + "." + method.getName());
        System.out.println("📝 [描述] " + description);
        if (logAnnotation.printArgs() && args != null && args.length > 0) {
            System.out.println("📝 [参数] " + Arrays.toString(args));
        }
    }

    private void after(Method method, Object result, Log logAnnotation) {
        if (logAnnotation.printResult()) {
            System.out.println("📝 [返回] " + (result != null ? result : "null"));
        }
        System.out.println("📝 ============ 日志结束 ============");
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
