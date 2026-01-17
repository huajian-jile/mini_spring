package liu.annotation.spring.aop;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Around {
    // 这里简单点，直接传入要拦截的类名或方法名
    // 为了简化，我们先拦截特定的类（比如 UserService）
    String value();
}