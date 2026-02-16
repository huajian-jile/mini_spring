package liu.spring.ioc;

import liu.spring.annotation.spring.ioc.Autowired;
import liu.db.SqlSession;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认的可列式 Bean 工厂（类似 Spring 的 DefaultListableBeanFactory）。
 */
public class DefaultListableBeanFactory implements BeanFactory, BeanDefinitionRegistry {

    private final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    private final Map<String, Object> singletonObjects = new HashMap<>();
    private final Set<String> singletonsCurrentlyInCreation = new HashSet<>();
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    private final Map<Object, Class<?>> proxyTargetTypeMap = new IdentityHashMap<>();
    private SqlSession sqlSession;

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition definition) {
        beanDefinitions.put(beanName, definition);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinitions.get(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitions.containsKey(beanName);
    }

    @Override
    public Set<String> getBeanDefinitionNames() {
        return new HashSet<>(beanDefinitions.keySet());
    }

    @Override
    public Object getBean(String name) {
        return doGetBean(name, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        return (T) getBeanByType(requiredType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> requiredType) {
        return (T) doGetBean(name, requiredType);
    }

    @Override
    public boolean containsBean(String name) {
        return singletonObjects.containsKey(name) || beanDefinitions.containsKey(name);
    }

    @Override
    public Object getBeanByType(Class<?> requiredType) {
        for (Object bean : singletonObjects.values()) {
            if (bean == null) continue;
            if (matchType(bean, requiredType)) return bean;
        }
        String name = resolveBeanNameByType(requiredType);
        if (name != null) return doGetBean(name, requiredType);
        throw new RuntimeException("找不到 Bean: " + requiredType.getName());
    }

    private String resolveBeanNameByType(Class<?> requiredType) {
        for (Map.Entry<String, BeanDefinition> e : beanDefinitions.entrySet()) {
            BeanDefinition bd = e.getValue();
            Class<?> clazz = bd.getBeanClass();
            if (requiredType.equals(clazz) || requiredType.isAssignableFrom(clazz)) return e.getKey();
            if (bd.isMapper() && requiredType.isInterface() && requiredType.equals(clazz)) return e.getKey();
        }
        return null;
    }

    private boolean matchType(Object bean, Class<?> requiredType) {
        if (Proxy.isProxyClass(bean.getClass())) {
            for (Class<?> intf : bean.getClass().getInterfaces()) {
                if (requiredType.equals(intf)) return true;
            }
            Class<?> target = proxyTargetTypeMap.get(bean);
            if (target != null && requiredType.isAssignableFrom(target)) return true;
        }
        return requiredType.isInstance(bean);
    }

    protected Object doGetBean(String name, Class<?> requiredType) {
        Object singleton = singletonObjects.get(name);
        if (singleton != null) return singleton;
        BeanDefinition bd = beanDefinitions.get(name);
        if (bd == null) throw new RuntimeException("未找到 Bean 定义: " + name);
        if (singletonsCurrentlyInCreation.contains(name)) throw new RuntimeException("检测到循环依赖，Bean: " + name);
        singletonsCurrentlyInCreation.add(name);
        try {
            singleton = bd.isMapper() ? createMapperBean(bd) : createBean(bd);
            if (bd.isSingleton()) singletonObjects.put(name, singleton);
            return singleton;
        } finally {
            singletonsCurrentlyInCreation.remove(name);
        }
    }

    private Object createMapperBean(BeanDefinition bd) {
        if (sqlSession == null) throw new IllegalStateException("SqlSession 未注入到 BeanFactory，无法创建 Mapper");
        return SqlSession.getMapper(bd.getBeanClass());
    }

    private Object createBean(BeanDefinition bd) {
        String beanName = bd.getBeanName();
        Class<?> clazz = bd.getBeanClass();
        Object bean;
        try {
            bean = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("实例化 Bean 失败: " + clazz.getName(), e);
        }
        populateBean(beanName, bean, clazz);
        bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
        Object wrapped = applyBeanPostProcessorsAfterInitialization(bean, beanName);
        if (wrapped != bean && Proxy.isProxyClass(wrapped.getClass())) {
            proxyTargetTypeMap.put(wrapped, clazz);
        }
        return wrapped;
    }

    private void populateBean(String beanName, Object bean, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Autowired.class)) continue;
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            try {
                field.set(bean, getBeanByType(fieldType));
            } catch (Exception e) {
                throw new RuntimeException("注入失败: " + clazz.getName() + "." + field.getName() + " -> " + fieldType.getName(), e);
            }
        }
    }

    private Object applyBeanPostProcessorsBeforeInitialization(Object bean, String beanName) {
        Object result = bean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            result = processor.postProcessBeforeInitialization(result, beanName);
            if (result == null) throw new RuntimeException("BeanPostProcessor 返回 null: " + processor.getClass().getName());
        }
        return result;
    }

    private Object applyBeanPostProcessorsAfterInitialization(Object bean, String beanName) {
        Object result = bean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            result = processor.postProcessAfterInitialization(result, beanName);
            if (result == null) throw new RuntimeException("BeanPostProcessor 返回 null: " + processor.getClass().getName());
        }
        return result;
    }

    public void preInstantiateSingletons() {
        for (String name : getBeanDefinitionNames()) {
            BeanDefinition bd = beanDefinitions.get(name);
            if (bd != null && bd.isSingleton() && !bd.isLazy()) getBean(name);
        }
    }

    public void registerSingleton(String beanName, Object singleton) {
        singletonObjects.put(beanName, singleton);
    }

    public void addBeanPostProcessor(BeanPostProcessor processor) {
        this.beanPostProcessors.add(processor);
    }

    public void setSqlSession(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    public Map<String, Object> getSingletonObjects() { return singletonObjects; }
    public Map<Object, Class<?>> getProxyTargetTypeMap() { return proxyTargetTypeMap; }
}
