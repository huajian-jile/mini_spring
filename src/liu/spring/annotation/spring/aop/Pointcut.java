package liu.spring.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 切点注解
 * 用于定义切入点表达式，支持多种匹配模式
 * 
 * 支持的表达式格式：
 * 1. 全类名匹配：liu.service.UserService
 * 2. 包路径通配：liu.service.*（匹配该包下所有类）
 * 3. 包路径递归：liu.service..**（匹配该包及子包下所有类）
 * 4. 注解匹配：@annotation(liu.spring.annotation.spring.aop.Log)
 * 5. execution 表达式：execution(* liu.service.*.*(..))
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pointcut {
    /**
     * 切点表达式
     */
    String value();
}

