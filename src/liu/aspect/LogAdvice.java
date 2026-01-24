package liu.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;

public class LogAdvice implements Advice {
    
    @Override
    public void before(Method method, Object[] args) {
        System.out.println("📝 日志开始 -> 方法: " + method.getName() + ", 参数: " + Arrays.toString(args));
    }

    @Override
    public void after(Method method, Object result) {
        System.out.println("📝 日志结束 -> 方法: " + method.getName() + ", 结果: " + result);
    }

    @Override
    public void afterThrowing(Method method, Throwable ex) {
        System.out.println("📝 日志异常 -> 方法: " + method.getName() + ", 异常: " + ex.getMessage());
    }
}