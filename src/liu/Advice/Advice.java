package liu.Advice;

import java.lang.reflect.Method;

// 定义一个通用的拦截器接口，统一 JDK 和 CGLIB 的处理逻辑
public interface Advice {
    Object invoke(Method method, Object[] args, Object target) throws Exception;
}