package liu.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 后置通知（finally 型，与 Spring 的 @After 一致）。
 * 在目标方法执行后（无论正常返回或异常）执行；
 * 方法签名可为 void after(Object target, Method method, Object[] args)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface After {
    String value();
}
