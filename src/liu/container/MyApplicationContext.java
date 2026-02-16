// liu/container/MyApplicationContext.java
package liu.container;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import liu.annotation.spring.aop.After;
import liu.annotation.spring.aop.AfterReturning;
import liu.annotation.spring.aop.AfterThrowing;
import liu.annotation.spring.aop.Around;
import liu.annotation.spring.aop.Aspect;
import liu.annotation.spring.aop.Before;
import liu.annotation.spring.aop.Order;
import liu.annotation.web.GetMapping;
import liu.annotation.web.PostMapping;
import liu.annotation.web.RequestMapping;
import liu.annotation.web.RestController;
import liu.annotation.spring.ioc.Controller;
import liu.annotation.spring.ioc.Mapper;
import liu.db.MyDataSource;
import liu.db.SqlSession;
import liu.container.aop.AdviceType;
import liu.container.aop.PointcutAdvisorEntry;
import liu.util.Handler;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * 应用上下文（类似 Spring 的 ApplicationContext）。
 * 职责：扫描、注册 Bean 定义、注册扩展（AOP/组件注解）、建立路由、启动服务器。
 * Bean 的创建与生命周期由 BeanFactory 管理，扩展新功能通过 BeanPostProcessor 与 ComponentAnnotationRegistry。
 */
public class MyApplicationContext {

    private final DefaultListableBeanFactory beanFactory;
    private final ComponentAnnotationRegistry componentRegistry;
    private final Map<String, Handler> handlerMapping = new HashMap<>();
    private final Map<Class<?>, Object> aspectMap = new HashMap<>();
    private final Map<String, Method> adviceMap = new HashMap<>();
    private final List<PointcutAdvisorEntry> pointcutAdvisorList = new ArrayList<>();

    public MyApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
        this.componentRegistry = new ComponentAnnotationRegistry();
    }

    /** 获取 BeanFactory，便于扩展（如注册新的 BeanPostProcessor、单例等） */
    public DefaultListableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    /** 获取组件注解注册表，便于扩展（如注册新的组件注解） */
    public ComponentAnnotationRegistry getComponentRegistry() {
        return componentRegistry;
    }

    /**
     * 刷新容器：扫描 -> 注册 Bean 定义 -> 初始化 AOP -> 注册后置处理器 -> 实例化 Bean -> 路由 -> 启动服务
     */
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
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @" + "Around " + method.getName());
                }
                if (method.isAnnotationPresent(Before.class)) {
                    String expression = method.getAnnotation(Before.class).value();
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.BEFORE, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @" + "Before " + method.getName());
                }
                if (method.isAnnotationPresent(After.class)) {
                    String expression = method.getAnnotation(After.class).value();
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.AFTER, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @" + "After " + method.getName());
                }
                if (method.isAnnotationPresent(AfterReturning.class)) {
                    String expression = method.getAnnotation(AfterReturning.class).value();
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.AFTER_RETURNING, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @" + "AfterReturning " + method.getName());
                }
                if (method.isAnnotationPresent(AfterThrowing.class)) {
                    String expression = method.getAnnotation(AfterThrowing.class).value();
                    pointcutAdvisorList.add(new PointcutAdvisorEntry(expression, method, AdviceType.AFTER_THROWING, order));
                    System.out.println("⚡️ AOP 配置: [" + expression + "] -> @" + "AfterThrowing " + method.getName());
                }
            }
        }
        System.out.println("✅ AOP 初始化完成，共 " + aspectMap.size() + " 个切面，" + pointcutAdvisorList.size() + " 个通知");
    }

    /** 只注册 Bean 定义，不创建实例；组件与 Mapper 通过可扩展的注册表识别 */
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
                if (clazz.isAnnotationPresent(Aspect.class)) {
                    continue;
                }
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
        Set<String> names = beanFactory.getBeanDefinitionNames();
        for (String name : names) {
            BeanDefinition bd = beanFactory.getBeanDefinition(name);
            if (bd == null || bd.isAspect() || bd.isMapper()) continue;
            Class<?> clazz = bd.getBeanClass();
            if (!clazz.isAnnotationPresent(RestController.class) && !clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }
            Object controller = beanFactory.getBean(name);
            String classLevelPath = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                classLevelPath = clazz.getAnnotation(RequestMapping.class).value();
            }
            for (Method method : clazz.getDeclaredMethods()) {
                String methodPath = "";
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    methodPath = method.getAnnotation(RequestMapping.class).value();
                }
                if (method.isAnnotationPresent(PostMapping.class)) {
                    methodPath = method.getAnnotation(PostMapping.class).value();
                }
                if (method.isAnnotationPresent(GetMapping.class)) {
                    methodPath = method.getAnnotation(GetMapping.class).value();
                }
                if (!methodPath.isEmpty()) {
                    String url = ("/" + classLevelPath + "/" + methodPath).replaceAll("/+", "/");
                    handlerMapping.put(url, new Handler(controller, method, url));
                    System.out.println("🗺️  映射: " + url + " -> " + method.getName());
                }
            }
        }
    }

    private void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8099), 0);
        server.createContext("/", new DispatcherHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("💻 服务器启动成功，监听端口: 8099");
    }

    private class DispatcherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            Handler handler = handlerMapping.get(path);
            try {
                if (handler != null) {
                    Object result = handler.method.invoke(handler.controller);
                    String response = result != null ? result.toString() : "Success";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        }
    }

    private static String toLowerFirstCase(String simpleName) {
        if (simpleName == null || simpleName.isEmpty()) return simpleName;
        char[] chars = simpleName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
    }

    // ---------- 对外 API（兼容原有用法） ----------

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        return (T) beanFactory.getBean(name);
    }

    public <T> T getBean(Class<T> requiredType) {
        return beanFactory.getBean(requiredType);
    }
}
