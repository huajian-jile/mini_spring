package liu.container.aop;

import liu.annotation.spring.aop.PrintCurrentTime;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 对带 @PrintCurrentTime 的方法在调用前后打印当前时间。
 * 通过 AopAdvisorRegistry 注册即可生效。
 */
public class PrintCurrentTimeMethodInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = getTargetMethod(invocation);
        PrintCurrentTime ann = method == null ? null : method.getAnnotation(PrintCurrentTime.class);
        if (ann == null) {
            return invocation.proceed();
        }

        String desc = ann.value() != null && !ann.value().isEmpty() ? ann.value() : method.getName();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                ann.format() != null && !ann.format().isEmpty() ? ann.format() : "yyyy-MM-dd HH:mm:ss");

        if (ann.before()) {
            printTime("调用前", desc, formatter);
        }

        Object result = invocation.proceed();

        if (ann.after()) {
            printTime("调用后", desc, formatter);
        }
        return result;
    }

    private void printTime(String when, String desc, DateTimeFormatter formatter) {
        String time = LocalDateTime.now().format(formatter);
        System.out.println("🕐 [PrintCurrentTime] " + when + " - " + desc + " | 当前时间: " + time);
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
