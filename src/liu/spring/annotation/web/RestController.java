// liu/inf/RestController.java
package liu.spring.annotation.web;

import liu.spring.annotation.spring.ioc.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 标记这是一个控制器，且返回的是数据（不是页面）
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component // 底层依然是组件
public @interface RestController {
    String value() default "";
}


