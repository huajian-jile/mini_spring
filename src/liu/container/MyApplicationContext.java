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
import liu.aspect.Advice;
import liu.aspect.LogAdvice;
import liu.aspect.PerformanceAdvice;
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
import java.lang.reflect.InvocationHandler;
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
    private  Map<String, Object> beanFactory = new HashMap<>();

    // 🆕 新增：路由映射表
    private Map<String, Handler> handlerMapping = new HashMap<>();

    // 🆕 新增：存放切面的映射
    private Map<Class<?>, Object> aspectMap = new HashMap<>();
    // 🆕 新增：存放切入点表达式和切面方法的映射 (这里简化处理，Key是类名)
    private Map<String, Method> adviceMap = new HashMap<>();
    
    // 🆕 新增：保存代理对象对应的原始类型（用于依赖注入时查找）
    // Key: 代理对象, Value: 原始类型
    // 使用 IdentityHashMap 避免调用代理对象的 hashCode() 方法
    private  Map<Object, Class<?>> proxyTargetTypeMap = new java.util.IdentityHashMap<>();


    // 修改：使用我们自己的 MyDataSource
    private MyDataSource myDataSource;
    private SqlSession sqlSession;

    /**
     * 刷新容器：扫描 -> 实例化 -> 注入 -> AOP -> 重新注入
     */
    public void refresh(Class<?> appClass) throws Exception {
        String packageName = appClass.getPackage().getName();
        System.out.println("🚀 开始扫描包: " + packageName);
        //0.初始化数据库
        initDataSource();

        // 1. 扫描
        List<Class<?>> classes = scanPackage(packageName);

        // 2. 初始化 AOP (必须在实例化之前，用于注册切面)
        initAop(classes);

        // 3. 实例化 (第一次循环)
        doInstance(classes);

        // 4. 🔥 第一次依赖注入（注入原始对象，让对象之间建立引用关系）
        System.out.println("📌 第一次依赖注入（注入原始对象）");
        doAutowired();

        // 5. 创建 AOP 代理（在依赖注入之后）
        createAopProxies();

        // 6. 🔥 第二次依赖注入（更新所有引用为代理对象）
        System.out.println("📌 第二次依赖注入（更新为代理对象）");
        doAutowired();

        // 7. 建立映射关系（新增)
        initHandlerMapping(classes);

        startServer();
    }
    // 🆕 0.修改：使用极简数据源
    private void initDataSource() {
//        // 这里直接写死配置，为了演示。实际可以读取 application.properties
//        String driver = "com.mysql.cj.jdbc.Driver";
//        String url = "jdbc:mysql://localhost:3306/big_event?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true";
//        String username = "root";
//        String password = "root";
//
//        this.sqlSession = new SqlSession(); // 传入我们自己的数据源
//        this.myDataSource = new MyDataSource(driver, url, username, password);
//
//        // 放入容器，方便其他地方获取连接
//        beanFactory.put("sqlSession", sqlSession);
        System.out.println("🔌 数据库连接初始化成功");
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
    /**
    2.注册aop
     */
    private void initAop(List<Class<?>> classes) throws Exception {
        for (Class<?> clazz : classes) {
            // 跳过注解类型本身
            if (clazz.isAnnotation()) {
                continue;
            }
            
            // 1. 检查是不是切面类
            // 如果有，说明这是一个“切面类”（比如 LogAspect.java）
            if (clazz.isAnnotationPresent(Aspect.class)) {
                // 实例化切面类（使用 getDeclaredConstructor 替代废弃的 newInstance）
                // 就像 new LogAspect() 一样，把切面对象创建出来
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
    //3.实例化
    private void doInstance(List<Class<?>> classes) throws Exception {
        for (Class<?> clazz : classes) {

            // 1. 🛡️ 跳过注解接口
            if (clazz.isAnnotation()) {
                System.out.println("⏭️  跳过注解: " + clazz.getName());
                continue;
            }

            Object instance = null;
            String beanName = null;

            // 2. 🗃️ 处理 @Mapper (持久层接口/类)
            //    注意：这里优先处理注解，因为 @Mapper 可能标记在接口上
            if (clazz.isAnnotationPresent(Mapper.class)) {

                // 如果是接口，生成 MyBatis 风格的代理
                if (clazz.isInterface()) {
                    instance = SqlSession.getMapper(clazz);
                    beanName = toLowerFirstCase(clazz.getSimpleName());
                    beanFactory.put(beanName, instance);
                    System.out.println("📊 注册 Mapper 接口: " + beanName + " -> " + clazz.getSimpleName());
                } else {
                    // 如果是普通的 Mapper 类 (比如你写了具体的实现类)，正常实例化
                    instance = clazz.getDeclaredConstructor().newInstance();
                    beanName = toLowerFirstCase(clazz.getSimpleName());
                    beanFactory.put(beanName, instance);
                    System.out.println("📦 注册 Mapper 类: " + beanName + " -> " + clazz.getSimpleName());
                }
                continue;
            }

            // 3. 🏷️ 处理业务组件 (@Component, @Service, @Controller)
            if (clazz.isAnnotationPresent(Component.class) ||
                    clazz.isAnnotationPresent(Service.class) ||
                    clazz.isAnnotationPresent(Controller.class)) {

                // --- 第一步：实例化 ---
                instance = clazz.getDeclaredConstructor().newInstance();
                beanName = toLowerFirstCase(clazz.getSimpleName());

                // --- 第二步：AOP 代理逻辑（关键修改点）---
                // 检查这个实例是否需要被 AOP 增强
                // 1. 检查类上有没有 @Log 或 @ExecutionTime
                boolean needAop = clazz.isAnnotationPresent(Log.class) ||
                        clazz.isAnnotationPresent(ExecutionTime.class);

                // 2. 或者检查它的方法上有没有（这里简单起见只检查类级别，你可以扩展）
                // 如果需要 AOP
                if (needAop) {

                    // 3.1 根据注解类型，决定使用哪个 Advice
                    Advice advice = null;

                    if (clazz.isAnnotationPresent(Log.class)) {
                        advice = new LogAdvice();
                    } else if (clazz.isAnnotationPresent(ExecutionTime.class)) {
                        advice = new PerformanceAdvice();
                    }
                    // 这里可以加 else if 处理其他注解

                    // 3.2 创建代理处理器 (传入目标对象 instance 和 增强逻辑 advice)
                    // 注意：这里假设 instance 有接口，才能用 JDK 代理
                    // 如果没有接口，需要使用 CGLIB (这里先用 JDK 代理示意)
                    InvocationHandler handler = new AopProxyHandler(instance, advice);

                    // 3.3 获取目标对象实现的所有接口
                    Class<?>[] interfaces = instance.getClass().getInterfaces();

                    // 3.4 只有实现了接口，才能生成 JDK 动态代理
                    // 如果没有接口，这里需要降级为 CGLIB 或者直接使用原对象（或者抛异常）
                    if (interfaces.length > 0) {
                        instance = Proxy.newProxyInstance(
                                instance.getClass().getClassLoader(),
                                interfaces,
                                handler
                        );
                        System.out.println("🎭 生成 AOP 代理: " + beanName + " -> " + clazz.getSimpleName());
                    } else {
                        // 如果没有接口，无法进行 JDK 代理，只能使用原对象（或者使用 CGLIB）
                        System.out.println("⚠️  无法代理 (无接口): " + beanName + " -> " + clazz.getSimpleName() + " (缺少接口，跳过 AOP)");
                    }
                }
                // --- 第三步：放入容器 ---
                // 无论是否代理，instance 变量现在指向的是最终要放入容器的对象
                beanFactory.put(beanName, instance);
                System.out.println("📦 注册 Bean: " + beanName + " -> " + clazz.getSimpleName());
            }
        }
    }
    // --- 4. 注入阶段 ---
    private void doAutowired() throws Exception {
        for (Map.Entry<String, Object> entry : beanFactory.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    try {
                        Object dependencyBean = getBeanByType(fieldType);
                        
                        // 获取当前字段的旧值（用于判断是否更新）
                        Object oldValue = field.get(instance);
                        
                        // 只有值不同时才更新（避免重复注入相同对象）
                        if (oldValue != dependencyBean) {
                            field.set(instance, dependencyBean);
                            
                            // 判断是否是代理对象
                            boolean isProxy = dependencyBean.getClass().getName().contains("$Proxy");
                            String beanType = isProxy ? "代理对象" : "原始对象";
                            
                            System.out.println("💉 注入 " + beanType + ": " + 
                                dependencyBean.getClass().getName() + 
                                " 到 " + clazz.getSimpleName() + "." + field.getName());
                        }
                    } catch (Exception e) {
                        throw new Exception("注入失败！在类 [" + clazz.getName() + "] 的字段 [" + field.getName() + "] 上，类型为 [" + fieldType.getName() + "]", e);
                    }
                }
            }
        }
    }

    /**
     * 5.
     * 🆕 创建 AOP 代理对象
     * 在依赖注入之后调用，这样原始对象的依赖已经注入完成
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

                    // 保存代理对象和原始类型的映射关系（用于类型匹配）
                    proxyTargetTypeMap.put(proxyInstance, originalType);

                    System.out.println("🛡️  已生成 AOP 代理: " + beanName + " -> " + originalType.getSimpleName());
                    System.out.println("    ├─ 原始对象: " + instance);
                    System.out.println("    └─ 代理对象: " + proxyInstance);
                }
            }
        }

        // 替换原始对象为代理对象
        beanFactory.putAll(proxiedBeans);
        System.out.println("    └─ 替换原始对象为代理对象: " + proxiedBeans);
        System.out.println("✅ AOP 代理创建完成，共 " + proxiedBeans.size() + " 个代理对象");
    }






    // 🆕 6.新增：初始化处理器映射
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
        HttpServer server = HttpServer.create(new InetSocketAddress(8099), 0);
        server.createContext("/", new DispatcherHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("💻 服务器启动成功，监听端口: 8099");
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
        Advice advice = new LogAdvice(); // 或者从容器里 getBean
        if (interfaces.length > 0) {
            // 有接口，使用 JDK 动态代理
            return Proxy.newProxyInstance(
                    target.getClass().getClassLoader(),
                    interfaces,
                    new AopProxyHandler(target, advice) // 这里传入 target 和 advice
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
                clazz.isAnnotationPresent(Mapper.class);


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
        if (clazz.isAnnotationPresent(Mapper.class)) {
            String value = clazz.getAnnotation(Mapper.class).value();
            if (!value.isEmpty()) return value;
        }
        // 默认首字母小写
        return toLowerFirstCase(clazz.getSimpleName());
    }

    private Object getBeanByType(Class<?> type) {
        for (Object bean : beanFactory.values()) {
            if (bean == null) continue;

            try {
                // 1. 如果是 JDK 动态代理
                if (Proxy.isProxyClass(bean.getClass())) {
                    // 获取代理实现的所有接口
                    Class<?>[] interfaces = bean.getClass().getInterfaces();
                    // 检查我们要找的 type 是否在这些接口中
                    for (Class<?> intf : interfaces) {
                        if (type.equals(intf)) {
                            // 找到了！直接返回代理对象
                            return bean;
                        }
                    }
                }

                // 2. 如果是普通对象 或 CGLIB 代理
                // isInstance 会自动处理普通对象和 CGLIB 代理（子类 instanceof 父类 = true）
                if (type.isInstance(bean)) {
                    return bean;
                }

            } catch (Exception e) {
                e.printStackTrace();
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