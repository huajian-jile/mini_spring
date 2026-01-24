package liu.aspect;

import java.lang.reflect.Method;

public class PerformanceAdvice implements Advice {
    private long startTime;

    @Override
    public void before(Method method, Object[] args) {
        startTime = System.currentTimeMillis();
        System.out.println("⏱️  性能监控 -> " + method.getName() + " 开始执行...");
    }

    @Override
    public void after(Method method, Object result) {
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("⏱️  性能监控 -> " + method.getName() + " 耗时: " + duration + "ms");
    }

    @Override
    public void afterThrowing(Method method, Throwable ex) {
        // 性能监控通常不关心异常，留空即可
    }
}