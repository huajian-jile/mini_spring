package liu.spring.annotation.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法返回值直接写回响应体（如 JSON），与 Spring 的 @ResponseBody 一致。
 * @RestController 标注的类中所有方法默认视为 @ResponseBody。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseBody {
}
