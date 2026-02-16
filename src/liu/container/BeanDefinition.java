package liu.container;

import java.lang.annotation.Annotation;

/**
 * Bean 定义，描述一个 Bean 的元数据（类似 Spring 的 BeanDefinition）。
 * 支持扩展：可标记是否为 Mapper、作用域等。
 */
public class BeanDefinition {

    public static final String SCOPE_SINGLETON = "singleton";
    public static final String SCOPE_PROTOTYPE = "prototype";

    /** Bean 名称 */
    private final String beanName;
    /** Bean 类型 */
    private final Class<?> beanClass;
    /** 作用域，默认单例 */
    private String scope = SCOPE_SINGLETON;
    /** 是否懒加载 */
    private boolean lazy = false;
    /** 是否为 Mapper 接口（需要代理创建） */
    private boolean mapper = false;
    /** 是否为切面类（不参与 AOP 代理，仅注册） */
    private boolean aspect = false;
    /** 组件注解类型（用于扩展识别） */
    private Class<? extends Annotation> componentAnnotation;

    public BeanDefinition(String beanName, Class<?> beanClass) {
        this.beanName = beanName;
        this.beanClass = beanClass;
    }

    public String getBeanName() {
        return beanName;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public boolean isMapper() {
        return mapper;
    }

    public void setMapper(boolean mapper) {
        this.mapper = mapper;
    }

    public boolean isAspect() {
        return aspect;
    }

    public void setAspect(boolean aspect) {
        this.aspect = aspect;
    }

    public Class<? extends Annotation> getComponentAnnotation() {
        return componentAnnotation;
    }

    public void setComponentAnnotation(Class<? extends Annotation> componentAnnotation) {
        this.componentAnnotation = componentAnnotation;
    }

    public boolean isSingleton() {
        return SCOPE_SINGLETON.equals(scope);
    }
}
