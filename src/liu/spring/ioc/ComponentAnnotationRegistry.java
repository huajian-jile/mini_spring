package liu.spring.ioc;

import liu.spring.annotation.spring.ioc.Component;
import liu.spring.annotation.spring.ioc.Controller;
import liu.spring.annotation.spring.ioc.Service;
import liu.spring.annotation.web.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 可扩展的组件注解注册表。
 */
public class ComponentAnnotationRegistry {

    private final List<Class<? extends Annotation>> componentAnnotations = new ArrayList<>();

    public ComponentAnnotationRegistry() {
        register(Component.class);
        register(Service.class);
        register(Controller.class);
        register(RestController.class);
    }

    public void register(Class<? extends Annotation> annotationType) {
        if (!componentAnnotations.contains(annotationType)) componentAnnotations.add(annotationType);
    }

    public boolean isComponent(Class<?> clazz) {
        if (clazz == null || clazz.isAnnotation()) return false;
        for (Class<? extends Annotation> ann : componentAnnotations) {
            if (clazz.isAnnotationPresent(ann)) return true;
        }
        return false;
    }

    public Class<? extends Annotation> getComponentAnnotation(Class<?> clazz) {
        for (Class<? extends Annotation> ann : componentAnnotations) {
            if (clazz.isAnnotationPresent(ann)) return ann;
        }
        return null;
    }

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
