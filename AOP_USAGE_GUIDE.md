# MiniSpring AOP 使用指南

## 概述

本 AOP 实现支持灵活的切点表达式匹配，类似 Spring AOP，但更轻量化。

## 核心注解

### @Aspect
标记一个类为切面类，必须配合 `@Component` 使用让容器管理。

### @Around
标记环绕通知方法，支持切点表达式。

### @Log
方法级别注解，自动记录方法调用日志（参数、返回值、异常）。

### @ExecutionTime
方法级别注解，自动统计方法执行时间。

## 切点表达式语法

### 1. 精确匹配类名
```java
@Around("liu.service.UserServiceImp")
```
只拦截 `UserServiceImp` 类的所有方法。

### 2. 包路径通配（单层）
```java
@Around("liu.service.*")
```
拦截 `liu.service` 包下所有类，**不包含子包**。

示例：
- ✅ 匹配：`liu.service.UserService`
- ✅ 匹配：`liu.service.OrderService`
- ❌ 不匹配：`liu.service.impl.UserServiceImpl`（子包）

### 3. 包路径递归通配
```java
@Around("liu.service..**")
```
拦截 `liu.service` 包及其**所有子包**下的类。

示例：
- ✅ 匹配：`liu.service.UserService`
- ✅ 匹配：`liu.service.impl.UserServiceImpl`
- ✅ 匹配：`liu.service.user.UserHandler`

### 4. 注解匹配（未来支持）
```java
@Around("@Log")
```
拦截所有标注了 `@Log` 注解的方法。

## 完整示例

### 步骤 1：创建切面类

```java
package liu.aspect;

import liu.annotation.spring.aop.Around;
import liu.annotation.spring.aop.Aspect;
import liu.annotation.spring.ioc.Component;

@Aspect      // 标记为切面
@Component   // 让容器管理
public class LogAspect {
    
    // 拦截 service 包下所有类
    @Around("liu.service.*")
    public Object logServiceMethods(Object[] args) {
        // 切面逻辑由 AopProxyHandler 自动处理
        // 这个方法主要用于标记切点
        return null;
    }
}
```

### 步骤 2：在业务类上使用注解

```java
package liu.service;

import liu.annotation.spring.aop.Log;
import liu.annotation.spring.aop.ExecutionTime;
import liu.annotation.spring.ioc.Service;

@Service
public class UserServiceImp implements IUserService {
    
    @Log(value = "查询所有用户", printArgs = true, printResult = true)
    @ExecutionTime(value = "数据库查询", threshold = 0)
    public List<User> getAllUsers() {
        // 业务逻辑
        return userMapper.findAll();
    }
}
```

### 步骤 3：运行查看效果

启动应用后，访问相关接口，会看到如下输出：

```
🔧 注册切面: LogAspect
⚡️ AOP 配置: 表达式 [liu.service.*] -> 切面方法 logServiceMethods
✅ AOP 初始化完成，共注册 1 个切面，1 个通知

🛡️  已生成 AOP 代理: userServiceImp -> UserServiceImp
    ├─ 切面方法: logServiceMethods
    ├─ 原始对象: 123456789
    └─ 代理对象: 987654321

📝 ============ 日志开始 ============
📝 [方法] UserServiceImp.getAllUsers
📝 [描述] 查询所有用户
📝 [参数] []
⏱️  [开始执行] 数据库查询
// ... 业务执行 ...
📝 [返回] [User{id=1, name='张三'}, ...]
📝 ============ 日志结束 ============
⏱️  [执行完成] 数据库查询 - 耗时: 245ms ✅
```

## 高级用法

### 多个切面

可以定义多个切面类，每个切面拦截不同的包或类：

```java
@Aspect
@Component
public class PerformanceAspect {
    @Around("liu.controller.*")
    public Object monitorController(Object[] args) {
        return null;
    }
}

@Aspect
@Component
public class SecurityAspect {
    @Around("liu.service..**")
    public Object checkSecurity(Object[] args) {
        return null;
    }
}
```

### 组合使用注解和切面

既可以使用 `@Log` / `@ExecutionTime` 注解，也可以使用切面拦截：

```java
// 方式1：注解方式（推荐，更灵活）
@Log("用户登录")
@ExecutionTime("登录操作")
public void login(String username, String password) {
    // ...
}

// 方式2：切面方式（统一拦截整个包）
@Around("liu.service.auth.*")
public Object logAuthService(Object[] args) {
    return null;
}
```

## 注意事项

1. **接口要求**：被代理的类必须实现接口（JDK 动态代理限制）
2. **切面执行顺序**：目前只支持单个切面匹配，后续可扩展优先级
3. **性能考虑**：代理会有轻微性能开销，建议合理使用
4. **切面不代理切面**：标注 `@Aspect` 的类本身不会被代理

## 与 Spring AOP 对比

| 特性 | MiniSpring AOP | Spring AOP |
|------|----------------|------------|
| 切点表达式 | 支持包路径通配 | 支持完整 AspectJ 表达式 |
| 代理方式 | JDK 动态代理 | JDK + CGLIB |
| 通知类型 | 环绕通知 | 前置/后置/环绕/异常/最终 |
| 注解支持 | @Log, @ExecutionTime | 完整 AspectJ 注解 |
| 学习成本 | 低 | 中 |

## 未来扩展

- [ ] 支持 CGLIB 代理（无接口类）
- [ ] 支持更多通知类型（@Before, @After, @AfterReturning）
- [ ] 支持切面优先级（@Order）
- [ ] 支持更复杂的切点表达式
- [ ] 支持参数绑定（JoinPoint）

