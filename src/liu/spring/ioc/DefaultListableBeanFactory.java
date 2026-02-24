package liu.spring.ioc;

import liu.spring.annotation.spring.ioc.Autowired;
import liu.spring.annotation.spring.ioc.PostConstruct;
import liu.spring.annotation.spring.ioc.Qualifier;
import liu.spring.annotation.spring.ioc.Value;
import liu.db.SqlSession;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认的可列式 Bean 工厂。支持：单例/原型作用域、懒加载、@PostConstruct、@Qualifier、@Value、构造器注入、BeanPostProcessor 排序。
 */
public class DefaultListableBeanFactory implements BeanFactory, BeanDefinitionRegistry {

    private final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    private final Map<String, Object> singletonObjects = new HashMap<>();
    private final Set<String> singletonsCurrentlyInCreation = new HashSet<>();
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    private final Map<Object, Class<?>> proxyTargetTypeMap = new IdentityHashMap<>();
    /** 简单属性源，用于 @Value("${key}") 解析，可后续扩展为 Environment */
    private final Map<String, String> propertySources = new HashMap<>();
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
        List<String> names = resolveBeanNamesByType(requiredType);
        if (names.isEmpty()) throw new RuntimeException("找不到 Bean: " + requiredType.getName());
        if (names.size() > 1) throw new RuntimeException("按类型找到多个 Bean: " + requiredType.getName() + " -> " + names + "，请使用 @Qualifier 指定名称");
        return doGetBean(names.get(0), requiredType);
    }

    /** 按类型解析 Bean 名称，返回所有匹配的名称 */
    public List<String> resolveBeanNamesByType(Class<?> requiredType) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> e : beanDefinitions.entrySet()) {
            BeanDefinition bd = e.getValue();
            Class<?> clazz = bd.getBeanClass();
            if (requiredType.equals(clazz) || requiredType.isAssignableFrom(clazz)) {
                names.add(e.getKey());
            }
        }
        return names;
    }

    private String resolveBeanNameByType(Class<?> requiredType) {
        List<String> names = resolveBeanNamesByType(requiredType);
        return names.isEmpty() ? null : names.get(0);
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
        BeanDefinition bd = beanDefinitions.get(name);
        if (bd == null) throw new RuntimeException("未找到 Bean 定义: " + name);
        if (bd.isSingleton()) {
            Object singleton = singletonObjects.get(name);
            if (singleton != null) return singleton;
        }
        if (singletonsCurrentlyInCreation.contains(name)) throw new RuntimeException("检测到循环依赖，Bean: " + name);
        singletonsCurrentlyInCreation.add(name);
        try {
            Object bean = bd.isMapper() ? createMapperBean(bd) : createBean(bd);
            if (bd.isSingleton()) singletonObjects.put(name, bean);
            return bean;
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
        Object bean = instantiateBean(beanName, clazz);
        populateBean(beanName, bean, clazz);
        invokePostConstruct(bean, clazz);
        bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
        Object wrapped = applyBeanPostProcessorsAfterInitialization(bean, beanName);
        if (wrapped != bean && Proxy.isProxyClass(wrapped.getClass())) {
            proxyTargetTypeMap.put(wrapped, clazz);
        }
        return wrapped;
    }

    /** 实例化：优先使用 @Autowired 构造器或唯一构造器进行构造器注入 */
    private Object instantiateBean(String beanName, Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> toUse = null;
        for (Constructor<?> c : constructors) {
            if (c.isAnnotationPresent(Autowired.class)) {
                if (toUse != null) throw new RuntimeException("存在多个 @Autowired 构造器: " + clazz.getName());
                toUse = c;
            }
        }
        if (toUse == null && constructors.length == 1) toUse = constructors[0];
        if (toUse != null && (toUse.getParameterCount() > 0 || toUse.isAnnotationPresent(Autowired.class))) {
            toUse.setAccessible(true);
            Class<?>[] paramTypes = toUse.getParameterTypes();
            java.lang.reflect.Parameter[] params = toUse.getParameters();
            Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                if (params[i].isAnnotationPresent(Qualifier.class)) {
                    String q = params[i].getAnnotation(Qualifier.class).value();
                    args[i] = getBean(q);
                } else if (params[i].isAnnotationPresent(Value.class)) {
                    String resolved = resolveValueString(params[i].getAnnotation(Value.class));
                    args[i] = convertToFieldType(resolved, paramTypes[i]);
                } else {
                    args[i] = getBeanByType(paramTypes[i]);
                }
            }
            try {
                return toUse.newInstance(args);
            } catch (Exception e) {
                throw new RuntimeException("构造器实例化失败: " + clazz.getName(), e);
            }
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("实例化 Bean 失败: " + clazz.getName(), e);
        }
    }

    private void invokePostConstruct(Object bean, Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(PostConstruct.class)) continue;
            if (m.getParameterCount() > 0) throw new RuntimeException("@PostConstruct 方法必须无参: " + clazz.getName() + "." + m.getName());
            m.setAccessible(true);
            try {
                m.invoke(bean);
            } catch (Exception e) {
                throw new RuntimeException("执行 @PostConstruct 失败: " + clazz.getName() + "." + m.getName(), e);
            }
        }
    }

    private void populateBean(String beanName, Object bean, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Autowired.class)) {
                Object dep = resolveDependency(field.getType(), field);
                try {
                    field.set(bean, dep);
                } catch (Exception e) {
                    throw new RuntimeException("注入失败: " + clazz.getName() + "." + field.getName(), e);
                }
            } else if (field.isAnnotationPresent(Value.class)) {
                String resolved = resolveValueString(field.getAnnotation(Value.class));
                Object value = convertToFieldType(resolved, field.getType());
                try {
                    field.set(bean, value);
                } catch (Exception e) {
                    throw new RuntimeException("@Value 注入失败: " + clazz.getName() + "." + field.getName(), e);
                }
            }
        }
    }

    private Object resolveDependency(Class<?> type, Field field) {
        if (field.isAnnotationPresent(Qualifier.class)) {
            String name = field.getAnnotation(Qualifier.class).value();
            return getBean(name);
        }
        return getBeanByType(type);
    }

    /** 解析 @Value 得到字符串：${key} 或 ${key:default}，未配置时用 default 或 key */
    private String resolveValueString(Value valueAnn) {
        String expr = valueAnn.value();
        if (expr == null || !expr.startsWith("${") || !expr.endsWith("}")) {
            return expr;
        }
        String inner = expr.substring(2, expr.length() - 1).trim();
        int colon = inner.indexOf(':');
        String key = colon > 0 ? inner.substring(0, colon).trim() : inner;
        String defaultVal = colon > 0 ? inner.substring(colon + 1).trim() : null;
        String resolved = propertySources.get(key);
        return resolved != null ? resolved : (defaultVal != null ? defaultVal : key);
    }

    /** 将 @Value 解析出的字符串转为字段/参数类型（支持 String、int、Integer、long、boolean 等） */
    private Object convertToFieldType(String value, Class<?> type) {
        if (value == null) return null;
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        return value;
    }

    /** 添加属性源（供 @Value 解析），可多次调用放入 key-value */
    public void setProperty(String key, String value) {
        propertySources.put(key, value);
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
        beanPostProcessors.sort(Comparator.comparingInt(p -> p instanceof Ordered ? ((Ordered) p).getOrder() : Ordered.LOWEST_PRECEDENCE));
    }

    public void setSqlSession(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    public Map<String, Object> getSingletonObjects() { return singletonObjects; }
    public Map<Object, Class<?>> getProxyTargetTypeMap() { return proxyTargetTypeMap; }
}
