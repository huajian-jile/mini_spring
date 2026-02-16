package liu.container;

import liu.annotation.spring.ioc.Component;
import liu.annotation.spring.ioc.Controller;
import liu.annotation.spring.ioc.Service;
import liu.annotation.web.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 可扩展的组件注解注册表。
 * 用于判断一个类是否为“组件”（需要实例化并放入容器）。
 * 新增组件注解时只需调用 register，无需修改扫描逻辑。
 */
public class ComponentAnnotationRegistry {

    private final List<Class<? extends Annotation>> componentAnnotations = new ArrayList<>();

    public ComponentAnnotationRegistry() {
        register(Component.class);
        register(Service.class);
        register(Controller.class);
        register(RestController.class);
    }

    /**
     * 注册一个组件注解类型（如 @Component、@Service、自定义 @MyComponent）
     */
    @SuppressWarnings("unchecked")
    public void register(Class<? extends Annotation> annotationType) {
        if (!componentAnnotations.contains(annotationType)) {
            componentAnnotations.add(annotationType);
        }
    }

    /**
     * 判断类是否被任意已注册的组件注解标注
     */
    public boolean isComponent(Class<?> clazz) {
        if (clazz == null || clazz.isAnnotation()) {
            return false;
        }
        for (Class<? extends Annotation> ann : componentAnnotations) {
            if (clazz.isAnnotationPresent(ann)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取类上第一个匹配的组件注解类型（用于 BeanDefinition 等）
     */
    public Class<? extends Annotation> getComponentAnnotation(Class<?> clazz) {
        for (Class<? extends Annotation> ann : componentAnnotations) {
            if (clazz.isAnnotationPresent(ann)) {
                return ann;
            }
        }
        return null;
    }

    /**
     * 获取建议的 Bean 名称：优先使用注解上的 value()，否则返回 null（调用方用类名首字母小写）
     */
    public String getSuggestedBeanName(Class<?> clazz) {
        Annotation a = null;
        for (Class<? extends Annotation> ann : componentAnnotations) {
            if (clazz.isAnnotationPresent(ann)) {
                a = clazz.getAnnotation(ann);
                break;
            }
        }
        if (a == null) return null;
        try {
            Method m = a.annotationType().getMethod("value");
            String v = (String) m.invoke(a);
            if (v != null && !v.isEmpty()) return v;
        } catch (Exception ignored) { }
        return null;
    }

    public List<Class<? extends Annotation>> getComponentAnnotations() {
        return new ArrayList<>(componentAnnotations);
    }
}
