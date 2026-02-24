package liu.spring.ioc;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import liu.spring.annotation.spring.aop.After;
import liu.spring.annotation.spring.aop.AfterReturning;
import liu.spring.annotation.spring.aop.AfterThrowing;
import liu.spring.annotation.spring.aop.Around;
import liu.spring.annotation.spring.aop.Aspect;
import liu.spring.annotation.spring.aop.Before;
import liu.spring.annotation.spring.aop.Order;
import liu.spring.annotation.web.GetMapping;
import liu.spring.annotation.web.PostMapping;
import liu.spring.annotation.web.RequestMapping;
import liu.spring.annotation.web.RestController;
import liu.spring.annotation.spring.ioc.Controller;
import liu.spring.annotation.spring.ioc.Mapper;
import liu.spring.container.AopBeanPostProcessor;
import liu.spring.container.aop.AdviceType;
import liu.spring.container.aop.PointcutAdvisorEntry;
import liu.spring.webmvc.DispatcherServlet;
import liu.spring.webmvc.RequestMappingHandlerAdapter;
import liu.spring.webmvc.RequestMappingHandlerMapping;
import liu.db.SqlSession;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * 应用上下文（IOC 入口）。扫描、注册 Bean 定义、初始化 AOP、注册后置处理器、实例化 Bean、路由、启动服务。
 */
public class MyApplicationContext {

    private final DefaultListableBeanFactory beanFactory;
    private final ComponentAnnotationRegistry componentRegistry;
    private HttpHandler dispatcherHandler;
    private final Map<Class<?>, Object> aspectMap = new HashMap<>();
    private final Map<String, Method> adviceMap = new HashMap<>();
    private final List<PointcutAdvisorEntry> pointcutAdvisorList = new ArrayList<>();

    public MyApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
        this.componentRegistry = new ComponentAnnotationRegistry();
    }

    public DefaultListableBeanFactory getBeanFactory() { return beanFactory; }
    public ComponentAnnotationRegistry getComponentRegistry() { return componentRegistry; }

    public void refresh(Class<?> appClass) throws Exception {
        String packageName = appClass.getPackage().getName();
        System.out.println("🚀 开始扫描包: " + packageName);
        initDataSource();
        List<Class<?>> classes = scanPackage(packageName);
        initAop(classes);
        registerBeanDefinitions(classes);
        registerProcessorsAndSingletons();
        beanFactory.preInstantiateSingletons();
        initHandlerMapping();
        startServer();
    }

    private void initDataSource() {
        System.out.println("🔌 数据库连接初始化成功");
    }

    private List<Class<?>> scanPackage(String packageName) throws Exception {
        List<Class<?>> classList = new ArrayList<>();
        String packagePath = packageName.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(packagePath);
        if (url != null) {
            File dir = new File(url.getFile());
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        classList.addAll(scanPackage(packageName + "." + file.getName()));
                    } else {
                        String fileName = file.getName();
                        if (fileName.endsWith(".class")) {
                            String className = fileName.substring(0, fileName.length() - 6);
                            String fullClassName = packageName + "." + className;
                            Class<?> clazz = classLoader.loadClass(fullClassName);
                            classList.add(clazz);
                        }
                    }
                }
            }
        }
        return classList;
    }

    private void initAop(List<Class<?>> classes) throws Exception {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotation()) continue;
            if (!clazz.isAnnotationPresent(Aspect.class)) continue;
            Object aspectInstance;
            try {
                aspectInstance = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("❌ 无法实例化切面类: " + clazz.getName());
                continue;
            }
            aspectMap.put(clazz, aspectInstance);
            int order = clazz.isAnnotationPresent(Order.class) ? clazz.getAnnotation(Order.class).value() : Integer.MAX_VALUE;
            System.out.println("🔧 注册切面: " + clazz.getSimpleName() + (order != Integer.MAX_VALUE ? " @Order(" + order + ")" : ""));
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Around.class)) {
                    String expression = method.getAnnotation(Around.class).value();
                    adviceMap.put(expression, method);
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.AROUND, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @Around " + method.getName());
                }
                if (method.isAnnotationPresent(Before.class)) {
                    String expression = method.getAnnotation(Before.class).value();
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.BEFORE, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @Before " + method.getName());
                }
                if (method.isAnnotationPresent(After.class)) {
                    String expression = method.getAnnotation(After.class).value();
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.AFTER, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @After " + method.getName());
                }
                if (method.isAnnotationPresent(AfterReturning.class)) {
                    String expression = method.getAnnotation(AfterReturning.class).value();
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.AFTER_RETURNING, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @AfterReturning " + method.getName());
                }
                if (method.isAnnotationPresent(AfterThrowing.class)) {
                    String expression = method.getAnnotation(AfterThrowing.class).value();
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.AFTER_THROWING, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @AfterThrowing " + method.getName());
                }
            }
        }
        System.out.println("✅ AOP 初始化完成，共 " + aspectMap.size() + " 个切面，" + pointcutAdvisorList.size() + " 个通知");
    }

    private void registerBeanDefinitions(List<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotation()) continue;
            String beanName;
            BeanDefinition bd;
            if (clazz.isAnnotationPresent(Mapper.class)) {
                beanName = getBeanName(clazz, true);
                bd = new BeanDefinition(beanName, clazz);
                bd.setMapper(clazz.isInterface());
                beanFactory.registerBeanDefinition(beanName, bd);
                System.out.println("📊 注册 Mapper: " + beanName + " -> " + clazz.getSimpleName());
                continue;
            }
            if (componentRegistry.isComponent(clazz)) {
                if (clazz.isAnnotationPresent(Aspect.class)) continue;
                beanName = getBeanName(clazz, false);
                bd = new BeanDefinition(beanName, clazz);
                bd.setComponentAnnotation(componentRegistry.getComponentAnnotation(clazz));
                beanFactory.registerBeanDefinition(beanName, bd);
                System.out.println("📦 注册 Bean: " + beanName + " -> " + clazz.getSimpleName());
            }
        }
    }

    private String getBeanName(Class<?> clazz, boolean isMapper) {
        if (isMapper && clazz.isAnnotationPresent(Mapper.class)) {
            String v = clazz.getAnnotation(Mapper.class).value();
            if (v != null && !v.isEmpty()) return v;
        }
        String suggested = componentRegistry.getSuggestedBeanName(clazz);
        if (suggested != null && !suggested.isEmpty()) return suggested;
        return toLowerFirstCase(clazz.getSimpleName());
    }

    private void registerProcessorsAndSingletons() {
        beanFactory.addBeanPostProcessor(AopBeanPostProcessor.of(aspectMap, adviceMap, pointcutAdvisorList));
        SqlSession sqlSession = new SqlSession();
        beanFactory.setSqlSession(sqlSession);
        beanFactory.registerSingleton("sqlSession", sqlSession);
    }

    private void initHandlerMapping() {
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        Set<String> names = beanFactory.getBeanDefinitionNames();
        for (String name : names) {
            BeanDefinition bd = beanFactory.getBeanDefinition(name);
            if (bd == null || bd.isAspect() || bd.isMapper()) continue;
            Class<?> clazz = bd.getBeanClass();
            if (!clazz.isAnnotationPresent(RestController.class) && !clazz.isAnnotationPresent(Controller.class)) continue;
            Object controller = beanFactory.getBean(name);
            handlerMapping.registerHandler(controller, clazz);
        }
        for (Map.Entry<String, List<liu.spring.webmvc.HandlerMethod>> e : handlerMapping.getExactLookup().entrySet()) {
            for (liu.spring.webmvc.HandlerMethod hm : e.getValue())
                System.out.println("🗺️  映射: " + e.getKey() + " " + java.util.Arrays.toString(hm.getHttpMethods()));
        }
        for (liu.spring.webmvc.HandlerMethod hm : handlerMapping.getPatternHandlers()) {
            System.out.println("🗺️  映射: " + hm.getPattern() + " (pattern)");
        }
        ArrayList<liu.spring.webmvc.HandlerMapping> mappings = new ArrayList<>();
        mappings.add(handlerMapping);
        ArrayList<liu.spring.webmvc.HandlerAdapter> adapters = new ArrayList<>();
        adapters.add(new RequestMappingHandlerAdapter());
        this.dispatcherHandler = new DispatcherServlet(mappings, adapters);
    }

    private void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8099), 0);
        server.createContext("/", dispatcherHandler);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("💻 服务器启动成功，监听端口: 8099");
    }

    private static String toLowerFirstCase(String simpleName) {
        if (simpleName == null || simpleName.isEmpty()) return simpleName;
        char[] chars = simpleName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) { return (T) beanFactory.getBean(name); }
    public <T> T getBean(Class<T> requiredType) { return beanFactory.getBean(requiredType); }
}
