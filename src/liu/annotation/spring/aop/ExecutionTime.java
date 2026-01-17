package liu.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 执行时间注解
 * 用于标记需要统计执行时间的方法
 * 会自动计算并打印方法的执行耗时
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecutionTime {
    /**
     * 描述信息
     */
    String value() default "";
    
    /**
     * 超过指定毫秒数时才打印（用于性能监控）
     * 默认 0 表示总是打印
     */
    long threshold() default 0;
}

