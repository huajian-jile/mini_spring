package liu.spring.container.aop;

import java.lang.reflect.Method;

/**
 * 反射调用切面方法，支持 (target, method, args)、(result)、(throwable) 等常见签名。
 */
public final class AspectInvocationHelper {

    public static void invokeAdvice(Object aspectInstance, Method adviceMethod,
                                    Object target, Method method, Object[] args,
                                    Object result, Throwable throwable) throws Throwable {
        if (aspectInstance == null || adviceMethod == null) return;
        adviceMethod.setAccessible(true);
        Class<?>[] paramTypes = adviceMethod.getParameterTypes();
        Object[] invokeArgs;
        if (paramTypes.length == 3
                && paramTypes[0] == Object.class
                && paramTypes[1] == Method.class
                && paramTypes[2] == Object[].class) {
            invokeArgs = new Object[]{target, method, args != null ? args : new Object[0]};
        } else if (paramTypes.length == 4
                && paramTypes[0] == Object.class
                && paramTypes[1] == Method.class
                && paramTypes[2] == Object[].class
                && (paramTypes[3] == Object.class || paramTypes[3] == Throwable.class)) {
            if (paramTypes[3] == Throwable.class) {
                invokeArgs = new Object[]{target, method, args != null ? args : new Object[0], throwable};
            } else {
                invokeArgs = new Object[]{target, method, args != null ? args : new Object[0], result};
            }
        } else if (paramTypes.length == 0) {
            invokeArgs = new Object[0];
        } else {
            return;
        }
        adviceMethod.invoke(aspectInstance, invokeArgs);
    }
}
