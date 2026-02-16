package liu.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 打印当前时间注解。
 * 用于在方法执行时自动打印当前时间（可配置执行前/后）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PrintCurrentTime {

    /**
     * 描述信息（会一起打印）
     */
    String value() default "";

    /**
     * 是否在方法执行前打印
     */
    boolean before() default true;

    /**
     * 是否在方法执行后打印
     */
    boolean after() default true;

    /**
     * 时间格式，默认 yyyy-MM-dd HH:mm:ss
     */
    String format() default "yyyy-MM-dd HH:mm:ss";
}
