package liu.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 切面/顾问执行顺序（与 Spring 的 @Order 一致）。
 * 数字越小越先执行；未标注时默认使用 Ordered.LOWEST_PRECEDENCE。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
    int value() default Integer.MAX_VALUE;
}
