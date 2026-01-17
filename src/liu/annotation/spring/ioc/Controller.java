package liu.annotation.spring.ioc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Controller.java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component // 底层还是继承自 Component，表示它也是一个 Bean
public @interface Controller {
    String value() default "";
}