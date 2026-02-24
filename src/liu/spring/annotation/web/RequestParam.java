package liu.spring.annotation.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 绑定请求参数（查询串或表单），与 Spring 的 @RequestParam 一致。
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String value() default "";
    String name() default "";
    boolean required() default true;
    String defaultValue() default "";
}
