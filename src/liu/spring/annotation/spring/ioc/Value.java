package liu.spring.annotation.spring.ioc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 简单值注入（占位符或默认值）。
 * 格式：${key} 或 ${key:default}，未配置时使用 default；无 default 则用 key 作为字面量。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    String value();
}
