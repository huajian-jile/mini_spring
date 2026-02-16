package liu.spring.annotation.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 环绕通知注解
 * 用于标记环绕通知方法，支持灵活的切点表达式
 * 
 * 支持的表达式格式：
 * 1. 全类名：liu.service.UserServiceImp
 * 2. 包路径通配：liu.service.*
 * 3. 包路径递归：liu.service..**
 * 4. 注解匹配：@Log 或 @ExecutionTime（匹配有这些注解的方法）
 * 
 * 示例：
 * @Around("liu.service.*")  // 拦截 service 包下所有类
 * @Around("liu.service..**") // 拦截 service 包及子包下所有类
 * @Around("@Log")            // 拦截标注了 @Log 的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Around {
    /**
     * 切点表达式
     */
    String value();
}
