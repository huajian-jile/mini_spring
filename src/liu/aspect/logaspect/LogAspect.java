package liu.aspect.logaspect;

import liu.annotation.spring.aop.Around;
import liu.annotation.spring.aop.Aspect;
import liu.annotation.spring.ioc.Component;

@Aspect // 标记这是切面
@Component // 让容器管理
public class LogAspect {
    
    // 这里传入 UserService 的全类名
    @Around("liu.service.UserService")
    public Object logTime(Object[] args) {
        // 这个方法可以实现自定义的切面逻辑
        // 目前我们的逻辑主要在 AopProxyHandler 中处理
        System.out.println("🔍 [LogAspect] 切面方法被调用");
        return null;
    }
}