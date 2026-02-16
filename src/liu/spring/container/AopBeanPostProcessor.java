package liu.spring.container;

import liu.spring.ioc.BeanPostProcessor;
import liu.spring.container.aop.AopAdvisorRegistry;
import liu.spring.container.aop.PointcutAdvisorEntry;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 * AOP 的 Bean 后置处理器（IOC 扩展点，实现 liu.spring.ioc.BeanPostProcessor）。
 */
public class AopBeanPostProcessor implements BeanPostProcessor {

    private final AopAdvisorRegistry advisorRegistry;

    public AopBeanPostProcessor(AopAdvisorRegistry advisorRegistry) {
        this.advisorRegistry = advisorRegistry;
    }

    /**
     * 根据 aspectMap、adviceMap、pointcutAdvisorList 构建 AopAdvisorRegistry。
     * 若 pointcutAdvisorList 非空则使用完整顾问列表（含 @Before/@After 等）；否则仅用 adviceMap（兼容旧 @Around）。
     */
    public static AopBeanPostProcessor of(Map<Class<?>, Object> aspectMap,
                                          Map<String, java.lang.reflect.Method> adviceMap,
                                          List<PointcutAdvisorEntry> pointcutAdvisorList) {
        AopAdvisorRegistry registry = new AopAdvisorRegistry();
        registry.registerAnnotationAdvice(liu.spring.annotation.spring.aop.Log.class, new liu.spring.container.aop.LogMethodInterceptor());
        registry.registerAnnotationAdvice(liu.spring.annotation.spring.aop.ExecutionTime.class, new liu.spring.container.aop.ExecutionTimeMethodInterceptor());
        registry.registerAnnotationAdvice(liu.spring.annotation.spring.aop.PrintCurrentTime.class, new liu.spring.container.aop.PrintCurrentTimeMethodInterceptor());
        if (pointcutAdvisorList != null && !pointcutAdvisorList.isEmpty()) {
            registry.setPointcutAdvisors(aspectMap, pointcutAdvisorList);
        } else {
            registry.setAspectMaps(aspectMap, adviceMap);
        }
        return new AopBeanPostProcessor(registry);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean == null) return null;
        Class<?> targetClass = bean.getClass();

        if (targetClass.isAnnotationPresent(liu.spring.annotation.spring.aop.Aspect.class)) {
            return bean;
        }

        if (!advisorRegistry.needsProxy(targetClass)) {
            return bean;
        }

        Object proxy = createProxy(bean);
        return proxy != null ? proxy : bean;
    }

    private Object createProxy(Object target) {
        Class<?>[] interfaces = target.getClass().getInterfaces();
        if (interfaces.length > 0) {
            return Proxy.newProxyInstance(
                    target.getClass().getClassLoader(),
                    interfaces,
                    new AopProxyHandler(target, advisorRegistry)
            );
        }
        // 无接口时使用 CGLIB 子类代理（需引入 cglib 依赖）
        try {
            Object proxy = CglibAopProxy.createProxy(target, advisorRegistry);
            return proxy != null ? proxy : target;
        } catch (NoClassDefFoundError e) {
            return target;
        }
    }
}
