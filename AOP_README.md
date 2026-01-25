# MiniSpring AOP 功能说明

## 功能概述

本项目实现了基础的 AOP（面向切面编程）功能，支持通过注解实现方法级别的拦截。

## 新增注解

### 1. @Log - 日志注解

用于自动记录方法的调用信息、参数和返回值。

**属性：**
- `value`: 日志描述信息（默认为空，使用方法名）
- `printArgs`: 是否打印参数（默认 true）
- `printResult`: 是否打印返回值（默认 true）

**使用示例：**

```java
@Log(value = "获取用户列表", printArgs = true, printResult = true)
public List<String> getUsers() {
    return userRepository.getAllUsers();
}
```

**输出示例：**
```
📝 ============ 日志开始 ============
📝 [方法] UserService.getUsers
📝 [描述] 获取用户列表
📝 [参数] []
📝 [返回] [user1, user2, user3]
📝 ============ 日志结束 ============
```

### 2. @ExecutionTime - 执行时间注解

用于自动统计并打印方法的执行耗时。

**属性：**
- `value`: 描述信息（默认为空，使用方法名）
- `threshold`: 时间阈值（毫秒），只有超过此阈值才打印（默认 0，总是打印）

**使用示例：**

```java
@ExecutionTime(value = "查询用户列表", threshold = 50)
public List<Map<String, Object>> getAllUsers() {
    return userMapper.findAll();
}
```

**输出示例：**
```
⏱️  [开始执行] 查询用户列表
⏱️  [执行完成] 查询用户列表 - 耗时: 123ms
```

- 耗时 < 500ms: 显示 ✅ 绿色标记
- 耗时 500-1000ms: 正常显示
- 耗时 > 1000ms: 显示 ⚠️ 警告标记，并转换为秒

## 组合使用

两个注解可以同时使用在一个方法上：

```java
@Log(value = "获取用户列表", printArgs = true, printResult = true)
@ExecutionTime(value = "查询用户列表", threshold = 0)
public List<String> getUsers() {
    return userRepository.getAllUsers();
}
```

## AOP 代理机制

### 代理创建

容器会自动检测以下情况并创建 AOP 代理：

1. 方法上标注了 `@Log` 注解
2. 方法上标注了 `@ExecutionTime` 注解
3. 类匹配了 `@Around` 切面规则

### 代理方式

- **JDK 动态代理**：当类实现了接口时使用（推荐）
- 目前不支持 CGLIB 代理（没有接口的类）

**建议：** 为了能够正常使用 AOP 功能，请确保需要代理的类实现了接口。

## 容器增强

### 刷新流程

AOP 功能已集成到容器的刷新流程中：

1. 扫描包
2. 初始化 AOP 切面
3. 实例化 Bean
4. **创建 AOP 代理**（新增）
5. 建立路由映射
6. 依赖注入
7. 启动服务器

### 使用示例

在 Service 层使用：

```java
@Service
public class UserService implements Ut {
    
    @Log(value = "获取用户列表", printArgs = true, printResult = true)
    @ExecutionTime(value = "查询用户列表", threshold = 0)
    public List<String> getUsers() {
        // 业务逻辑
        return userRepository.getAllUsers();
    }
}
```

在 Controller 层使用：

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    @GetMapping("/list")
    @Log(value = "查询用户列表接口", printArgs = false, printResult = true)
    @ExecutionTime(value = "用户列表查询", threshold = 0)
    public List<Map<String, Object>> listUsers() {
        return userService.getAllUsers();
    }
}
```

## 测试接口

启动应用后，可以访问以下接口测试 AOP 功能：

1. **用户列表**: GET http://localhost:8080/user/list
   - 测试日志和执行时间统计

2. **慢速操作**: GET http://localhost:8080/user/slow
   - 测试执行时间阈值功能（耗时 > 200ms）

3. **创建用户**: POST http://localhost:8080/user/create
   - 测试日志功能

## 技术细节

### AopProxyHandler

核心的动态代理处理器，实现了 `InvocationHandler` 接口：

- 在方法执行前检测注解
- 执行前置逻辑（打印日志、记录开始时间）
- 调用目标方法
- 执行后置逻辑（打印返回值、统计耗时）
- 处理异常情况

### 容器 AOP 支持

`MyApplicationContext` 增强：

- `initAop()`: 初始化切面配置
- `createAopProxies()`: 批量创建代理对象
- `needsProxy()`: 判断是否需要代理
- `createProxy()`: 创建 JDK 动态代理

## 注意事项

1. **接口实现**: 需要代理的类必须实现接口，否则无法使用 JDK 动态代理
2. **性能考虑**: 代理会增加少量性能开销，建议合理使用阈值参数
3. **切面本身不代理**: 标注了 `@Aspect` 的类不会被代理
4. **代理时机**: 代理在依赖注入之前创建，确保注入的是代理对象

## 扩展建议

未来可以增强的功能：

1. 支持 CGLIB 代理（无接口类）
2. 支持更多切点表达式（如正则、包路径）
3. 支持前置、后置、异常通知等更多类型
4. 支持切面优先级和顺序控制
5. 集成更强大的日志框架


┌─────────────────┐
│  1. 扫描包      │
└────────┬────────┘
│
┌────────▼────────┐
│  2. 初始化 AOP   │  ← 注册切面和切入点
└────────┬────────┘
│
┌────────▼────────┐
│  3. 实例化 Bean  │  ← target = new UserService()
└────────┬────────┘
│
┌────────▼────────┐
│  4. 依赖注入    │  ← target.userMapper = userMapperBean ✅
└────────┬────────┘
│
┌────────▼────────┐
│  5. 创建 AOP 代理│  ← proxy = Proxy.newInstance(target)
│                 │    此时 target 的依赖已经注入完成
└────────┬────────┘
│
┌────────▼────────┐
│  6. 路由映射    │
└────────┬────────┘
│
┌────────▼────────┐
│  7. 启动服务器  │
└─────────────────┘

----确保 AOP 代理的生成必须在 HandlerMapping 初始化 之前完成。
----你的 main 方法能打印日志，是因为你直接调用了容器返回的代理对象。
你的 Controller 不打印日志，是因为 DispatcherServlet（或你写的请求分发器）拿到的 UserController 实例是原始对象，不是代理对象。
去检查 initHandlerMapping() 方法，确保你从容器里拿实例时，走的是 getBean() 流程！


run方法--->刷新容器，进入容器断点执行---->扫描包，也就是启动类的路径---->初始化数据库---->扫描所有包---->注册切面---->
注入依赖---->生成代理对象---->路径映射---->启动容器

为什么控制器不走代理对象?：在main里面反而走代理对象，原因在于getBean(" userServiceImp ")是直接拿到代理对象的，


            public Object getBean(String name) {
            Object target = beanFactory.get(name); // 拿到原始对象
            // 👇 检查是否有注解，如果有，返回代理
            if (hasAspect(target)) {
            return Proxy.newProxyInstance(..., new AopProxyHandler(target));
            }
            return target;
            }


而MyApplicationContext 里，初始化 Controller 的逻辑（initHandlerMapping）是这样的
            // 🆕 新增：初始化处理器映射
            private void initHandlerMapping(List<Class<?>> classes) {
            
            Object controller = getBeanByType(UserController.class);
            
            // 如果 getBeanByType `写得不好，它可能直接返回了 new 出来的实例，而不是 getBean 里的那个代理实例。`

结果：handlerMapping 里存的是一个全新的、原始的 UserController 对象。这个对象没有经过 getBean() 的逻辑，
所以没有被包装成代理。请求进来时，直接调用原始对象的方法。❌ AOP 失效。


打印日志:
         我在找: liu.Controller.UserController
         我手里有个Bean，它的类型是: jdk.proxy2.$Proxy9
         匹配结果: false
         我在找: liu.Controller.UserController
         我手里有个Bean，它的类型是: liu.Controller.UserController
         匹配结果: true


JDK代理无法通过类类型匹配



我们真的会写代码吗？我们只是会用框架。
ssm--->
spring---->springmvc(springweb)---->mybatis
不会造轮子和会，不想造是两回事。



1. 实例化
   ├─ userService (原始)
   └─ userController

2. 第一次注入（原始对象）
   └─ userController.userService = userService (原始) ✓

3. 创建 AOP 代理
   ├─ userServiceProxy = createProxy(userService)
   └─ beanFactory["userService"] = userServiceProxy

4. 第二次注入（代理对象）
   └─ userController.userService = userServiceProxy ✓✓

5. 结果
   ├─ getBean("userService") → 代理对象 ✓
   └─ controller.userService → 代理对象 ✓