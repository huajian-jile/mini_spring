package liu.spring.ioc;

import java.lang.annotation.Annotation;

/**
 * Bean 定义，描述一个 Bean 的元数据（类似 Spring 的 BeanDefinition）。
 */
public class BeanDefinition {

    public static final String SCOPE_SINGLETON = "singleton";
    public static final String SCOPE_PROTOTYPE = "prototype";

    private final String beanName;
    private final Class<?> beanClass;
    private String scope = SCOPE_SINGLETON;
    private boolean lazy = false;
    private boolean mapper = false;
    private boolean aspect = false;
    private Class<? extends Annotation> componentAnnotation;

    public BeanDefinition(String beanName, Class<?> beanClass) {
        this.beanName = beanName;
        this.beanClass = beanClass;
    }

    public String getBeanName() { return beanName; }
    public Class<?> getBeanClass() { return beanClass; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public boolean isLazy() { return lazy; }
    public void setLazy(boolean lazy) { this.lazy = lazy; }
    public boolean isMapper() { return mapper; }
    public void setMapper(boolean mapper) { this.mapper = mapper; }
    public boolean isAspect() { return aspect; }
    public void setAspect(boolean aspect) { this.aspect = aspect; }
    public Class<? extends Annotation> getComponentAnnotation() { return componentAnnotation; }
    public void setComponentAnnotation(Class<? extends Annotation> componentAnnotation) { this.componentAnnotation = componentAnnotation; }
    public boolean isSingleton() { return SCOPE_SINGLETON.equals(scope); }
}
