package liu.container.aop;

/**
 * 方法拦截器（类似 Spring 的 MethodInterceptor）。
 * 新增 AOP 能力时只需实现此接口并注册到 AopAdvisorRegistry，无需改代理核心代码。
 */
public interface MethodInterceptor {

    /**
     * 环绕拦截：可前置/后置处理，通过 invocation.proceed() 进入下一环或执行目标方法
     */
    Object invoke(MethodInvocation invocation) throws Throwable;
}
