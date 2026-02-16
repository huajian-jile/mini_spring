package liu.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 返回通知（与 Spring 的 @AfterReturning 一致）。
 * 仅在目标方法正常返回后执行；
 * 方法签名可为 void afterReturning(Object target, Method method, Object[] args, Object result)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterReturning {
    String value();
}
