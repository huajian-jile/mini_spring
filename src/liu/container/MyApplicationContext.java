// liu/container/MyApplicationContext.java
package liu.container;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import liu.annotation.spring.aop.Around;
import liu.annotation.spring.aop.Aspect;
import liu.annotation.spring.aop.Log;
import liu.annotation.spring.aop.ExecutionTime;
import liu.annotation.spring.ioc.*;
import liu.db.MyDataSource;
import liu.db.SqlSession;
import liu.annotation.web.GetMapping;
import liu.annotation.web.PostMapping;
import liu.annotation.web.RequestMapping;
import liu.annotation.web.RestController;
import liu.util.Handler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 应用上下文，即 IoC 容器
 */
public class MyApplicationContext {

    // 存放 Bean 的工厂
    private Map<String, Object> beanFactory = new HashMap<>();

    // 🆕 新增：路由映射表
    private Map<String, Handler> handlerMapping = new HashMap<>();

    // 🆕 新增：存放切面的映射
    private Map<Class<?>, Object> aspectMap = new HashMap<>();
    // 🆕 新增：存放切入点表达式和切面方法的映射 (这里简化处理，Key是类名)
    private Map<String, Method> adviceMap = new HashMap<>();
    
    // 🆕 新增：保存代理对象对应的原始类型（用于依赖注入时查找）
    // Key: 代理对象, Value: 原始类型
    private Map<Object, Class<?>> proxyTargetTypeMap = new HashMap<>();
    
    // 🆕 新增：保存代理对象对应的原始对象（用于获取字段信息）
    // Key: 代理对象, Value: 原始对象
    private Map<Object, Object> proxyTargetMap = new HashMap<>();


    // 修改：使用我们自己的 MyDataSource
    private MyDataSource myDataSource;
    private SqlSession sqlSession;

    /**
     * 一行启动！模仿 Spring Boot
     */
    public static MyApplicationContext run(Class<?> appClass, String... args) {
        // 检查注解
        if (!appClass.isAnnotationPresent(SpringBootApplication.class)) {
            throw new RuntimeException("启动类必须包含 @SpringBootApplication 注解");
        }

        // 创建上下文并刷新
        MyApplicationContext context = new MyApplicationContext();
        try {
            context.refresh(appClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return context;
    }
    /**
     * 刷新容器：扫描 -> 实例化 -> 注入
     */
    public void refresh(Class<?> appClass) throws Exception {
        String packageName = appClass.getPackage().getName();
        System.out.println("🚀 开始扫描包: " + packageName);
        //0.初始化数据库
        initDataSource();

        // 1. 扫描
        List<Class<?>> classes = scanPackage(packageName);

        // 2. 初始化 AOP (必须在实例化之前)
        initAop(classes);

        // 3. 实例化 (第一次循环)
        doInstance(classes);

        // 4. 创建 AOP 代理（在实例化之后，依赖注入之前）
        createAopProxies();

        // 5. 建立映射关系（新增)
        initHandlerMapping(classes);

        // 6. 注入 (第二次循环)
        doAutowired();

        startServer();
    }

    /**
     * 🆕 新增：初始化 AOP 相关注解
     * 在 refresh 方法中，扫描完类之后，实例化之前调用
     */
    private void initAop(List<Class<?>> classes) throws Exception {
        for (Class<?> clazz : classes) {
            // 跳过注解类型本身
            if (clazz.isAnnotation()) {
                continue;
            }
            
            // 1. 检查是不是切面类
            if (clazz.isAnnotationPresent(Aspect.class)) {
                // 实例化切面类（使用 getDeclaredConstructor 替代废弃的 newInstance）
                Object aspectInstance = null;
                try {
                    aspectInstance = clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    System.err.println("❌ 无法实例化切面类: " + clazz.getName() + " - " + e.getMessage());
                    continue;
                }
                
                aspectMap.put(clazz, aspectInstance);
                System.out.println("🔧 注册切面: " + clazz.getSimpleName());

                // 2. 解析切面类里的方法
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Around.class)) {
                        Around around = method.getAnnotation(Around.class);
                        // 简单的切入点表达式：这里我们传入的是类的全名，比如 "liu.service.UserService"
                        String targetClassName = around.value();
                        adviceMap.put(targetClassName, method);
                        System.out.println("⚡️ AOP 绑定: 拦截 " + targetClassName + " -> 使用切面方法 " + method.getName());
                    }
                }
            }
        }
    }
    // 🆕 修改：使用极简数据源
    private void initDataSource() {
        // 这里直接写死配置，为了演示。实际可以读取 application.properties
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/big_event?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true";
        String username = "root";
        String password = "root";

        this.sqlSession = new SqlSession(); // 传入我们自己的数据源
        this.myDataSource = new MyDataSource(driver, url, username, password);

        // 放入容器，方便其他地方获取连接
        beanFactory.put("sqlSession", sqlSession);
        System.out.println("🔌 数据库连接初始化成功");
    }





    // 🆕 新增：初始化处理器映射
    private void initHandlerMapping(List<Class<?>> classes) {
        try {
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(RestController.class) ||
                        clazz.isAnnotationPresent(Controller.class)) {

                    Object controller = getBeanByType(clazz);

                    // 获取类级别的 RequestMapping (如果有)
                    String classLevelPath = "";
                    if (clazz.isAnnotationPresent(RequestMapping.class)) {
                        classLevelPath = clazz.getAnnotation(RequestMapping.class).value();
                    }

                    // 遍历所有方法
                    for (Method method : clazz.getDeclaredMethods()) {
                        // 检查方法上是否有 Mapping 注解
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
                            // 组合类路径和方法路径
                            String url = ("/" + classLevelPath + "/" + methodPath)
                                    .replaceAll("/+", "/");
                            handlerMapping.put(url, new Handler(controller, method, url));
                            System.out.println("🗺️  映射: " + url + " -> " + method.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🆕 新增：启动 HTTP 服务器
    private void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new DispatcherHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("💻 服务器启动成功，监听端口: 8080");
    }

    // 🆕 新增：请求分发处理器
    class DispatcherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            Handler handler = handlerMapping.get(path);

            try {
                if (handler != null) {
                    // 调用对应的方法
                    Object result = handler.method.invoke(handler.controller);
                    String response = result != null ? result.toString() : "Success";

                    // 写回响应
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    // 404
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
    // --- 1. 扫描阶段 ---
    private List<Class<?>> scanPackage(String packageName) throws Exception {
        List<Class<?>> classList = new ArrayList<>();
        String packagePath = packageName.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(packagePath);

        if (url != null) {
            File dir = new File(url.getFile());
            for (File file : dir.listFiles()) {
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
        return classList;
    }


    private void doInstance(List<Class<?>> classes) throws Exception {
        for (Class<?> clazz : classes) {

            // 1. 🛡️ 第一优先级：如果是注解（Annotation），直接跳过
            //    因为 @Controller, @Service 等注解上都有 @Component，
            //    如果不跳过，下面的逻辑会试图去实例化这些注解接口，导致报错
            if (clazz.isAnnotation()) {
                System.out.println("⏭️  跳过注解: " + clazz.getName());
                continue;
            }
            
            // 跳过接口和抽象类
            if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                // 接口会在 @Repository 处理时生成代理
                continue;
            }

            // 2. 🗃️ 第二优先级：处理 @Repository (包含接口和普通类)
            if (clazz.isAnnotationPresent(Repository.class)) {

                // 如果是接口，生成 MyBatis 代理
                if (clazz.isInterface()) {
                    Object mapperProxy = SqlSession.getMapper(clazz);
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    beanFactory.put(beanName, mapperProxy);
                    System.out.println("📊 注册 Mapper: " + beanName + " -> " + clazz.getSimpleName());

                } else {
                    // 如果是普通的 Repository 类，正常实例化
                    try {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        String beanName = toLowerFirstCase(clazz.getSimpleName());
                        beanFactory.put(beanName, instance);
                        System.out.println("📦 注册 Bean: " + beanName + " -> " + clazz.getSimpleName());
                    } catch (Exception e) {
                        System.err.println("❌ 无法实例化: " + clazz.getName() + " - " + e.getMessage());
                    }
                }
                continue; // 处理完跳过，防止重复处理
            }

            // 3. 🏷️ 第三优先级：处理其他组件 (@Component, @Service, @Controller)
            //    注意：这里 Repository 已经处理过了，所以不用担心 Repository 接口跑进来
            if (clazz.isAnnotationPresent(Component.class) ||
                    clazz.isAnnotationPresent(Service.class) ||
                    clazz.isAnnotationPresent(Controller.class)) {

                try {
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    beanFactory.put(beanName, instance);
                    System.out.println("📦 注册 Bean: " + beanName + " -> " + clazz.getSimpleName());
                } catch (Exception e) {
                    System.err.println("❌ 无法实例化: " + clazz.getName() + " - " + e.getMessage());
                }
            }
        }
    }
    /**
     * 🆕 创建 AOP 代理对象
     * 在所有 Bean 实例化之后，依赖注入之前调用
     */
    private void createAopProxies() {
        Map<String, Object> proxiedBeans = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : beanFactory.entrySet()) {
            String beanName = entry.getKey();
            Object instance = entry.getValue();
            
            // 跳过切面类本身
            if (instance.getClass().isAnnotationPresent(Aspect.class)) {
                continue;
            }
            
            // 检查是否需要创建代理
            if (needsProxy(instance)) {
                Class<?> originalType = instance.getClass(); // 保存原始类型
                
                // 查找对应的切面方法（如果有）
                Method aspectMethod = adviceMap.get(originalType.getName());
                Object aspectInstance = null;
                if (aspectMethod != null) {
                    // 获取切面实例
                    for (Object obj : aspectMap.values()) {
                        if (aspectMethod.getDeclaringClass().isAssignableFrom(obj.getClass())) {
                            aspectInstance = obj;
                            break;
                        }
                    }
                }
                
                Object proxyInstance = createProxy(instance, aspectMethod, aspectInstance);
                
                // 只有成功创建代理才替换（如果没有接口，createProxy 返回原对象）
                if (proxyInstance != instance) {
                    proxiedBeans.put(beanName, proxyInstance);
                    
                    // 保存代理对象和原始类型的映射关系
                    proxyTargetTypeMap.put(proxyInstance, originalType);
                    // 保存代理对象和原始对象的映射关系（重要！）
                    proxyTargetMap.put(proxyInstance, instance);
                    
                    System.out.println("🛡️  已生成 AOP 代理: " + beanName + " -> " + originalType.getSimpleName());
                }
            }
        }
        
        // 替换原始对象为代理对象
        beanFactory.putAll(proxiedBeans);
    }
    
    /**
     * 判断一个对象是否需要创建代理
     */
    private boolean needsProxy(Object instance) {
        // 1. 检查类上是否有 @Aspect 注解的方法匹配
        Method aspectMethod = adviceMap.get(instance.getClass().getName());
        if (aspectMethod != null) {
            return true;
        }
        
        // 2. 检查类中的方法是否有 @Log 或 @ExecutionTime 注解
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Log.class) || 
                method.isAnnotationPresent(ExecutionTime.class)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 🆕 创建代理对象（支持接口代理和 CGLIB 代理）
     */
    private Object createProxy(Object target, Method aspectMethod, Object aspectInstance) {
        // 检查是否实现了接口
        Class<?>[] interfaces = target.getClass().getInterfaces();
        
        if (interfaces.length > 0) {
            // 有接口，使用 JDK 动态代理
            return Proxy.newProxyInstance(
                    target.getClass().getClassLoader(),
                    interfaces,
                    new AopProxyHandler(target, aspectMethod, aspectInstance)
            );
        } else {
            // 没有接口，暂时不支持 CGLIB，返回原对象
            // 在实际 Spring 中会使用 CGLIB 进行代理
            System.out.println("⚠️  警告: " + target.getClass().getSimpleName() + 
                " 没有实现接口，无法使用 JDK 动态代理，建议实现接口");
            return target;
        }
    }
    
    /**
     * 🆕 执行切面逻辑（用于 @Around 注解）
     */
    private Object invokeAdvice(Object target, Method method, Object[] args, Method aspectMethod) throws Throwable {
        // 1. 获取切面实例
        Object aspectInstance = null;
        for (Object obj : aspectMap.values()) {
            if (aspectMethod.getDeclaringClass().isAssignableFrom(obj.getClass())) {
                aspectInstance = obj;
                break;
            }
        }

        // 2. 执行环绕通知（在这里写你的日志和耗时代码）
        long startTime = System.currentTimeMillis();
        System.out.println("📝 日志开始: 正在执行 " + method.getName() + " 方法...");

        // 3. 执行目标方法
        Object result = method.invoke(target, args);

        // 4. 执行结束
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("📝 日志结束: 方法执行耗时 " + duration + "ms");

        return result;
    }
    // --- 3. 注入阶段 ---
    private void doAutowired() throws Exception {
        for (Map.Entry<String, Object> entry : beanFactory.entrySet()) {
            Object instance = entry.getValue();
            
            // 🆕 关键修复：如果是代理对象，获取原始对象来处理字段注入
            Object targetInstance = proxyTargetMap.getOrDefault(instance, instance);
            Class<?> clazz = targetInstance.getClass();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    try {
                        // 这里会抛出上面的 RuntimeException
                        Object dependencyBean = getBeanByType(fieldType);
                        // 注意：这里要设置到原始对象上，而不是代理对象
                        field.set(targetInstance, dependencyBean);
                        System.out.println("💉 注入: " + dependencyBean.getClass().getName() + " 到 " + clazz.getSimpleName() + "." + field.getName());
                    } catch (Exception e) {
                        throw new Exception("注入失败！在类 [" + clazz.getName() + "] 的字段 [" + field.getName() + "] 上，类型为 [" + fieldType.getName() + "]", e);
                    }
                }
            }
        }
    }

    // --- 工具方法 ---

    // 判断类是否为组件
    private boolean isComponent(Class<?> clazz) {
        // 如果这是一个注解接口，则跳过，不要实例化它。
        if (clazz.isAnnotation()) {
            return false;
        }

        return clazz.isAnnotationPresent(Component.class) ||
                clazz.isAnnotationPresent(Controller.class) ||
                clazz.isAnnotationPresent(Service.class) ||
                clazz.isAnnotationPresent(Repository.class);


    }

    // 获取 Bean 名称
    private String getBeanName(Class<?> clazz) {
        // 优先使用注解里的名字
        if (clazz.isAnnotationPresent(Component.class)) {
            String value = clazz.getAnnotation(Component.class).value();
            if (!value.isEmpty()) return value;
        }
        if (clazz.isAnnotationPresent(Controller.class)) {
            String value = clazz.getAnnotation(Controller.class).value();
            if (!value.isEmpty()) return value;
        }
        if (clazz.isAnnotationPresent(Service.class)) {
            String value = clazz.getAnnotation(Service.class).value();
            if (!value.isEmpty()) return value;
        }
        if (clazz.isAnnotationPresent(Repository.class)) {
            String value = clazz.getAnnotation(Repository.class).value();
            if (!value.isEmpty()) return value;
        }
        // 默认首字母小写
        return toLowerFirstCase(clazz.getSimpleName());
    }

    // 根据类型获取 Bean
    private Object getBeanByType(Class<?> type) {
        for (Object bean : beanFactory.values()) {
            // 1. 直接类型匹配
            if (type.isAssignableFrom(bean.getClass())) {
                return bean;
            }
            
            // 2. 如果是代理对象，检查原始类型
            Class<?> originalType = proxyTargetTypeMap.get(bean);
            if (originalType != null && type.isAssignableFrom(originalType)) {
                return bean;
            }
        }
        throw new RuntimeException("找不到 Bean: " + type.getName() + "，请检查是否添加了 @Component 或其衍生注解");
    }

    // 首字母小写
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
    }

    // 对外获取 Bean
    public <T> T getBean(String name) {
        return (T) beanFactory.get(name);
    }

    public <T> T getBean(Class<T> clazz) {
        return (T) getBeanByType(clazz);
    }
}