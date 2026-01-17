package liu.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志注解
 * 用于标记需要打印日志的方法
 * 会自动记录方法的调用信息、参数和返回值
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    /**
     * 日志描述信息
     */
    String value() default "";
    
    /**
     * 是否打印参数
     */
    boolean printArgs() default true;
    
    /**
     * 是否打印返回值
     */
    boolean printResult() default true;
}

