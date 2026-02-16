package liu.container.aop;

import liu.annotation.spring.aop.Advice;
import liu.aspect.logaspect.PointcutMatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AOP 顾问/拦截器注册中心（与 Spring 对齐）。
 * 支持：① 方法注解增强（可带 @Order）；② 切面增强（@Before/@After/@AfterReturning/@AfterThrowing/@Around + 方法级切点）；③ 链按 order 排序。
 */
public class AopAdvisorRegistry {

    private final Map<Class<? extends Annotation>, MethodInterceptor> annotationInterceptors = new LinkedHashMap<>();
    private final Map<Class<? extends Annotation>, Integer> annotationOrders = new LinkedHashMap<>();
    private final List<PointcutAdvisorEntry> pointcutAdvisors = new ArrayList<>();
    private final Map<Class<?>, Object> aspectInstances = new LinkedHashMap<>();

    /** 注册方法注解增强，默认 order = 0 */
    public void registerAnnotationAdvice(Class<? extends Annotation> annotationType, MethodInterceptor interceptor) {
        registerAnnotationAdvice(annotationType, interceptor, 0);
    }

    /** 注册方法注解增强并指定顺序（数字越小越先执行） */
    public void registerAnnotationAdvice(Class<? extends Annotation> annotationType, MethodInterceptor interceptor, int order) {
        annotationInterceptors.put(annotationType, interceptor);
        annotationOrders.put(annotationType, order);
    }

    /** 兼容旧用法：仅 @Around，按 adviceMap 生成顾问列表 */
    public void setAspectMaps(Map<Class<?>, Object> aspectMap, Map<String, Method> adviceMap) {
        aspectInstances.clear();
        pointcutAdvisors.clear();
        if (aspectMap != null) aspectInstances.putAll(aspectMap);
        if (adviceMap != null) {
            for (Map.Entry<String, Method> e : adviceMap.entrySet()) {
                Method m = e.getValue();
                Object instance = aspectInstances.get(m.getDeclaringClass());
                if (instance instanceof Advice) {
                    pointcutAdvisors.add(new PointcutAdvisorEntry(e.getKey(), m, AdviceType.AROUND, Ordered.LOWEST_PRECEDENCE));
                }
            }
        }
    }

    /** 设置完整顾问列表（含 @Before/@After/@AfterReturning/@AfterThrowing/@Around + order） */
    public void setPointcutAdvisors(Map<Class<?>, Object> aspectMap, List<PointcutAdvisorEntry> advisors) {
        aspectInstances.clear();
        pointcutAdvisors.clear();
        if (aspectMap != null) aspectInstances.putAll(aspectMap);
        if (advisors != null) pointcutAdvisors.addAll(advisors);
    }

    /** 构建拦截器链（方法级切点 + 按 order 排序） */
    public List<MethodInterceptor> getInterceptors(Object target, Method method) {
        if (target == null || method == null) return new ArrayList<>();
        Class<?> targetClass = target.getClass();
        List<OrderedItem> items = new ArrayList<>();

        for (Map.Entry<Class<? extends Annotation>, MethodInterceptor> e : annotationInterceptors.entrySet()) {
            if (method.isAnnotationPresent(e.getKey())) {
                int order = annotationOrders.getOrDefault(e.getKey(), 0);
                items.add(new OrderedItem(order, e.getValue()));
            }
        }

        for (PointcutAdvisorEntry entry : pointcutAdvisors) {
            if (!PointcutMatcher.matches(entry.getExpression(), targetClass, method)) continue;
            Object aspectInstance = aspectInstances.get(entry.getAdviceMethod().getDeclaringClass());
            if (aspectInstance == null) continue;
            MethodInterceptor interceptor = createInterceptor(entry, aspectInstance);
            if (interceptor != null) {
                items.add(new OrderedItem(entry.getOrder(), interceptor));
            }
        }

        items.sort(Comparator.comparingInt(OrderedItem::getOrder));
        List<MethodInterceptor> chain = new ArrayList<>(items.size());
        for (OrderedItem item : items) chain.add(item.interceptor);
        return chain;
    }

    private MethodInterceptor createInterceptor(PointcutAdvisorEntry entry, Object aspectInstance) {
        switch (entry.getAdviceType()) {
            case BEFORE:
                return new AspectBeforeMethodInterceptor(aspectInstance, entry.getAdviceMethod(), entry.getOrder());
            case AFTER:
                return new AspectAfterMethodInterceptor(aspectInstance, entry.getAdviceMethod(), entry.getOrder());
            case AFTER_RETURNING:
                return new AspectAfterReturningMethodInterceptor(aspectInstance, entry.getAdviceMethod(), entry.getOrder());
            case AFTER_THROWING:
                return new AspectAfterThrowingMethodInterceptor(aspectInstance, entry.getAdviceMethod(), entry.getOrder());
            case AROUND:
                if (aspectInstance instanceof Advice) {
                    return new AspectMethodInterceptor((Advice) aspectInstance, entry.getOrder());
                }
                return null;
            default:
                return null;
        }
    }

    public boolean needsProxy(Class<?> targetClass) {
        if (targetClass == null || targetClass.isAnnotation()) return false;
        for (PointcutAdvisorEntry entry : pointcutAdvisors) {
            String expr = entry.getExpression();
            if (expr != null && expr.startsWith("@")) {
                for (Method m : targetClass.getDeclaredMethods()) {
                    if (PointcutMatcher.matches(expr, targetClass, m)) return true;
                }
            } else if (PointcutMatcher.matches(expr, targetClass, null)) {
                return true;
            }
        }
        for (Method m : targetClass.getDeclaredMethods()) {
            for (Class<? extends Annotation> ann : annotationInterceptors.keySet()) {
                if (m.isAnnotationPresent(ann)) return true;
            }
        }
        return false;
    }

    public Map<Class<? extends Annotation>, MethodInterceptor> getAnnotationInterceptors() {
        return new LinkedHashMap<>(annotationInterceptors);
    }

    private static class OrderedItem {
        final int order;
        final MethodInterceptor interceptor;
        OrderedItem(int order, MethodInterceptor interceptor) {
            this.order = order;
            this.interceptor = interceptor;
        }
        int getOrder() { return order; }
    }
}
