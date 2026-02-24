package liu.spring.annotation.spring.ioc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 生命周期回调：在依赖注入完成后、Bean 使用前执行（与 JSR-250 / Spring 一致）。
 * 标注在无参方法上，容器会在初始化阶段反射调用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstruct {
}
