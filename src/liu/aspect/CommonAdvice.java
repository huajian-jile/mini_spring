package liu.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;

public class CommonAdvice implements Advice {

    private boolean needLog;
    private boolean needTime;

    public CommonAdvice(boolean needLog, boolean needTime) {
        this.needLog = needLog;
        this.needTime = needTime;
    }

    @Override
    public void before(Method method, Object[] args) {
        // 如果需要日志，打印开始
        if (needLog) {
            System.out.println("📝 日志开始 -> 方法: " + method.getName() + ", 参数: " + Arrays.toString(args));
        }
        
        // 如果需要耗时统计，记录开始时间
        if (needTime) {
            // 这里需要一个地方存 startTime
            // 简单起见，可以用 ThreadLocal，或者一个临时的成员变量（注意线程安全）
            // 为了简单演示，假设我们有一个工具类或直接打印
            System.out.println("⏱️  " + method.getName() + " 开始执行...");
            // 这里设置 startTime...
        }
    }

    @Override
    public void after(Method method, Object result) {
        // 如果需要日志，打印结果
        if (needLog) {
            System.out.println("📝 日志结束 -> 方法: " + method.getName() + ", 结果: " + result);
        }

        // 如果需要耗时统计，计算并打印
        if (needTime) {
            // 这里获取 startTime，计算差值
            System.out.println("⏱️  " + method.getName() + " 执行完毕，耗时: ... ms");
        }
    }

    @Override
    public void afterThrowing(Method method, Throwable ex) {
        if (needLog) {
            System.out.println("📝 日志异常 -> " + ex.getMessage());
        }
    }
}