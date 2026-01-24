package liu.aspect;

import java.lang.reflect.Method;

public interface Advice {
    void before(Method method, Object[] args);
    void after(Method method, Object result);
    void afterThrowing(Method method, Throwable ex);
}