package liu.spring.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异常通知（与 Spring 的 @AfterThrowing 一致）。
 * 仅在目标方法抛出异常时执行；
 * 方法签名可为 void afterThrowing(Object target, Method method, Object[] args, Throwable ex)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterThrowing {
    String value();
}
