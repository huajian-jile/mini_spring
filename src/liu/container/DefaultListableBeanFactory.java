package liu.container;

import liu.annotation.spring.ioc.Autowired;
import liu.db.SqlSession;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认的可列式 Bean 工厂（类似 Spring 的 DefaultListableBeanFactory）。
 * 负责 Bean 的完整生命周期：实例化 -> 属性注入 -> BeanPostProcessor 前置 -> 后置（如 AOP）-> 放入单例缓存。
 * 支持通过 BeanPostProcessor 扩展，无需修改本类即可添加新能力。
 */
public class DefaultListableBeanFactory implements BeanFactory, BeanDefinitionRegistry {

    /** Bean 定义 */
    private final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    /** 单例 Bean 缓存（一级缓存） */
    private final Map<String, Object> singletonObjects = new HashMap<>();
    /** 正在创建中的 Bean 名称（用于检测循环依赖，简化版可仅做提示） */
    private final Set<String> singletonsCurrentlyInCreation = new HashSet<>();
    /** Bean 后置处理器列表（可扩展） */
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    /** 代理对象 -> 原始类型，用于 getBeanByType 时匹配接口 */
    private final Map<Object, Class<?>> proxyTargetTypeMap = new IdentityHashMap<>();

    private SqlSession sqlSession;

    // ---------- BeanDefinitionRegistry ----------

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

    // ---------- BeanFactory ----------

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
            if (matchType(bean, requiredType)) {
                return bean;
            }
        }
        String name = resolveBeanNameByType(requiredType);
        if (name != null) {
            return doGetBean(name, requiredType);
        }
        throw new RuntimeException("找不到 Bean: " + requiredType.getName());
    }

    /** 根据类型解析 Bean 名称（用于依赖注入时按类型获取并触发创建） */
    private String resolveBeanNameByType(Class<?> requiredType) {
        for (Map.Entry<String, BeanDefinition> e : beanDefinitions.entrySet()) {
            BeanDefinition bd = e.getValue();
            Class<?> clazz = bd.getBeanClass();
            if (requiredType.equals(clazz) || requiredType.isAssignableFrom(clazz)) {
                return e.getKey();
            }
            if (bd.isMapper() && requiredType.isInterface() && requiredType.equals(clazz)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** 类型匹配：支持 JDK 代理（按接口）和普通实例 */
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

    // ---------- 生命周期核心 ----------

    /**
     * 获取 Bean，若未创建则按生命周期创建并缓存
     */
    protected Object doGetBean(String name, Class<?> requiredType) {
        Object singleton = singletonObjects.get(name);
        if (singleton != null) {
            return singleton;
        }

        BeanDefinition bd = beanDefinitions.get(name);
        if (bd == null) {
            throw new RuntimeException("未找到 Bean 定义: " + name);
        }

        if (singletonsCurrentlyInCreation.contains(name)) {
            throw new RuntimeException("检测到循环依赖，Bean: " + name);
        }

        singletonsCurrentlyInCreation.add(name);
        try {
            if (bd.isMapper()) {
                singleton = createMapperBean(bd);
            } else {
                singleton = createBean(bd);
            }
            if (bd.isSingleton()) {
                singletonObjects.put(name, singleton);
            }
            return singleton;
        } finally {
            singletonsCurrentlyInCreation.remove(name);
        }
    }

    /** 创建 Mapper 代理 Bean */
    private Object createMapperBean(BeanDefinition bd) {
        if (sqlSession == null) {
            throw new IllegalStateException("SqlSession 未注入到 BeanFactory，无法创建 Mapper");
        }
        return SqlSession.getMapper(bd.getBeanClass());
    }

    /**
     * 完整生命周期：实例化 -> 属性注入 -> postProcessBefore -> postProcessAfter
     */
    private Object createBean(BeanDefinition bd) throws RuntimeException {
        String beanName = bd.getBeanName();
        Class<?> clazz = bd.getBeanClass();

        // 1. 实例化
        Object bean;
        try {
            bean = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("实例化 Bean 失败: " + clazz.getName(), e);
        }

        // 2. 属性注入（@Autowired）
        populateBean(beanName, bean, clazz);

        // 3. BeanPostProcessor 前置
        bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        // 4. BeanPostProcessor 后置（AOP 等在此返回代理）
        Object wrapped = applyBeanPostProcessorsAfterInitialization(bean, beanName);

        // 若返回了代理，记录代理 -> 原始类型，便于 getBeanByType 按接口查找
        if (wrapped != bean && Proxy.isProxyClass(wrapped.getClass())) {
            proxyTargetTypeMap.put(wrapped, clazz);
        }

        return wrapped;
    }

    /** 属性注入 */
    private void populateBean(String beanName, Object bean, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Autowired.class)) continue;
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            try {
                Object dependency = getBeanByType(fieldType);
                field.set(bean, dependency);
            } catch (Exception e) {
                throw new RuntimeException("注入失败: " + clazz.getName() + "." + field.getName() + " -> " + fieldType.getName(), e);
            }
        }
    }

    private Object applyBeanPostProcessorsBeforeInitialization(Object bean, String beanName) {
        Object result = bean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            result = processor.postProcessBeforeInitialization(result, beanName);
            if (result == null) {
                throw new RuntimeException("BeanPostProcessor 返回 null: " + processor.getClass().getName());
            }
        }
        return result;
    }

    private Object applyBeanPostProcessorsAfterInitialization(Object bean, String beanName) {
        Object result = bean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            result = processor.postProcessAfterInitialization(result, beanName);
            if (result == null) {
                throw new RuntimeException("BeanPostProcessor 返回 null: " + processor.getClass().getName());
            }
        }
        return result;
    }

    /** 预实例化所有单例 Bean（在 refresh 时由 Context 调用） */
    public void preInstantiateSingletons() {
        for (String name : getBeanDefinitionNames()) {
            BeanDefinition bd = beanDefinitions.get(name);
            if (bd != null && bd.isSingleton() && !bd.isLazy()) {
                getBean(name);
            }
        }
    }

    /** 直接注册单例实例（用于 SqlSession、DataSource 等非扫描得到的 Bean） */
    public void registerSingleton(String beanName, Object singleton) {
        singletonObjects.put(beanName, singleton);
    }

    // ---------- 扩展点：注册处理器与依赖 ----------

    /** 添加 Bean 后置处理器（如 AOP、自定义注解处理） */
    public void addBeanPostProcessor(BeanPostProcessor processor) {
        this.beanPostProcessors.add(processor);
    }

    public void setSqlSession(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    /** 供外部获取单例缓存 */
    public Map<String, Object> getSingletonObjects() {
        return singletonObjects;
    }

    public Map<Object, Class<?>> getProxyTargetTypeMap() {
        return proxyTargetTypeMap;
    }
}
