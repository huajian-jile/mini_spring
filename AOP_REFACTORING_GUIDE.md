# MiniSpring AOP 重构说明

## 核心设计理念

采用标准的 AOP 设计模式，通过 `Advice` 接口实现切面逻辑的解耦。

## 架构图

```
┌─────────────────────────────────────────────────────────┐
│                    AOP 架构                              │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────┐                                        │
│  │  @Aspect    │  ← 标记切面类                          │
│  │  LogAspect  │                                        │
│  │             │                                        │
│  │ implements  │                                        │
│  │   Advice    │  ← 实现 Advice 接口，定义切面逻辑     │
│  └──────┬──────┘                                        │
│         │                                                │
│         │ @Around("liu.service.*")                      │
│         │  ↓ 切点表达式                                 │
│         │                                                │
│  ┌──────▼──────────────┐                                │
│  │  PointcutMatcher    │  ← 切点匹配器                 │
│  │  - liu.service.*    │     判断类是否匹配表达式      │
│  │  - liu.service..** │                                │
│  └──────┬──────────────┘                                │
│         │                                                │
│         │ 匹配成功                                      │
│         ▼                                                │
│  ┌─────────────────┐                                    │
│  │ AopProxyHandler │  ← 代理处理器                     │
│  │                 │     使用 Advice 执行切面逻辑      │
│  │  - target       │                                    │
│  │  - advice       │                                    │
│  └─────────────────┘                                    │
│         │                                                │
│         │ JDK 动态代理                                  │
│         ▼                                                │
│  ┌─────────────┐                                        │
│  │  Proxy      │  ← 代理对象                           │
│  │             │     注入到其他 Bean                    │
│  └─────────────┘                                        │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## 核心组件

### 1. Advice 接口

定义切面的通用行为，包括：
- `before()` - 前置通知
- `after()` - 后置通知
- `afterThrowing()` - 异常通知
- `around()` - 环绕通知（默认实现）

```java
public interface Advice {
    default void before(Object target, Method method, Object[] args) {}
    default void after(Object target, Method method, Object[] args, Object result) {}
    default void afterThrowing(Object target, Method method, Object[] args, Throwable throwable) {}
    default Object around(Object target, Method method, Object[] args) throws Throwable {
        // 默认实现：调用前置、执行方法、调用后置
    }
}
```

### 2. AopProxyHandler

统一的代理处理器，使用 `Advice` 而不是硬编码：

```java
public class AopProxyHandler implements InvocationHandler {
    private final Object target;  // 目标对象
    private final Advice advice;  // 通知逻辑
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 1. 处理 Object 方法
        // 2. 处理 @Log/@ExecutionTime 注解
        // 3. 使用 Advice 执行切面逻辑
        return advice.around(target, method, args);
    }
}
```

### 3. @Aspect 切面类

实现 `Advice` 接口，提供具体的切面逻辑：

```java
@Aspect
@Component
public class LogAspect implements Advice {
    
    @Around("liu.service.*")  // 切点表达式
    public Object pointcut() {
        return null;  // 只用于标记切点
    }
    
    @Override
    public void before(Object target, Method method, Object[] args) {
        System.out.println("前置通知：" + method.getName());
    }
    
    @Override
    public void after(Object target, Method method, Object[] args, Object result) {
        System.out.println("后置通知：" + method.getName());
    }
}
```

## 使用示例

### 示例 1：简单的日志切面

```java
@Aspect
@Component
public class SimpleLogAspect implements Advice {
    
    @Around("liu.service.*")
    public Object pointcut() {
        return null;
    }
    
    @Override
    public void before(Object target, Method method, Object[] args) {
        System.out.println(">>> 开始执行: " + method.getName());
    }
    
    @Override
    public void after(Object target, Method method, Object[] args, Object result) {
        System.out.println("<<< 执行完成: " + method.getName());
    }
}
```

### 示例 2：性能监控切面

```java
@Aspect
@Component
public class PerformanceAspect implements Advice {
    
    @Around("liu.service..**")  // 拦截 service 包及子包
    public Object pointcut() {
        return null;
    }
    
    @Override
    public Object around(Object target, Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = method.invoke(target, args);
        long duration = System.currentTimeMillis() - start;
        
        if (duration > 100) {
            System.out.println("⚠️ 慢方法: " + method.getName() + " 耗时 " + duration + "ms");
        }
        
        return result;
    }
}
```

### 示例 3：异常捕获切面

```java
@Aspect
@Component
public class ExceptionAspect implements Advice {
    
    @Around("liu.controller.*")  // 拦截 controller 包
    public Object pointcut() {
        return null;
    }
    
    @Override
    public void afterThrowing(Object target, Method method, Object[] args, Throwable throwable) {
        System.err.println("❌ 异常捕获: " + method.getName());
        System.err.println("   异常类型: " + throwable.getClass().getSimpleName());
        System.err.println("   异常信息: " + throwable.getMessage());
        // 可以在这里记录日志、发送告警等
    }
}
```

## 切点表达式支持

| 表达式 | 说明 | 示例 |
|--------|------|------|
| `包名.类名` | 精确匹配 | `liu.service.UserService` |
| `包名.*` | 包内所有类 | `liu.service.*` |
| `包名..**` | 包及子包所有类 | `liu.service..**` |
| `*类名` | 类名通配 | `*Service` |

## 执行流程

```
1. 容器启动
   ↓
2. 扫描 @Aspect 类
   ↓
3. 实例化切面类（Advice 实例）
   ↓
4. 解析 @Around 注解，提取切点表达式
   ↓
5. 实例化所有 Bean
   ↓
6. 第一次依赖注入（原始对象）
   ↓
7. 创建 AOP 代理
   - 使用 PointcutMatcher 匹配切点
   - 找到匹配的 Advice
   - 创建代理：new AopProxyHandler(target, advice)
   ↓
8. 第二次依赖注入（更新为代理对象）
   ↓
9. 启动完成
```

## 与旧版本对比

### 旧版本（硬编码）

```java
// ❌ 问题：硬编码类名，耦合度高
@Around("liu.service.UserService")
public void logTime(String pjp) {
    // 逻辑写死在 AopProxyHandler 中
}
```

### 新版本（解耦）

```java
// ✅ 优点：使用 Advice 接口，逻辑解耦
@Aspect
@Component
public class LogAspect implements Advice {
    
    @Around("liu.service.*")  // 支持通配符
    public Object pointcut() {
        return null;
    }
    
    @Override
    public void before(Object target, Method method, Object[] args) {
        // 自定义前置逻辑
    }
}
```

## 优势

1. **解耦**：切面逻辑通过 `Advice` 接口定义，不再硬编码
2. **灵活**：支持多种切点表达式，一次配置拦截多个类
3. **可扩展**：易于添加新的切面类型和通知类型
4. **标准化**：遵循 AOP 设计模式，接近 Spring AOP 的使用方式
5. **可组合**：可以定义多个切面，每个切面处理不同的关注点

## 注意事项

1. 切面类必须实现 `Advice` 接口
2. 切面类必须添加 `@Aspect` 和 `@Component` 注解
3. 被代理的类必须实现接口（JDK 动态代理限制）
4. `@Around` 方法只用于标记切点，不会被实际调用
5. 切面的具体逻辑在 `before/after/around` 方法中实现

