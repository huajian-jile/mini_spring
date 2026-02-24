package liu.spring.container.aop;

/**
 * 暴露当前 AOP 代理（与 Spring 的 AopContext 一致）。
 * 在代理方法内部可通过 currentProxy() 获取当前代理对象，用于自调用时仍走切面。
 * 需在创建代理时设置 exposeProxy=true 才会写入 ThreadLocal（当前实现默认暴露）。
 */
public final class AopContext {

    private static final ThreadLocal<Object> currentProxy = new ThreadLocal<>();

    /** 获取当前线程正在执行的 AOP 代理对象，未在代理调用中则返回 null */
    public static Object currentProxy() {
        return currentProxy.get();
    }

    /** 内部使用：代理调用前设置 */
    public static void setCurrentProxy(Object proxy) {
        if (proxy != null) {
            currentProxy.set(proxy);
        } else {
            currentProxy.remove();
        }
    }

    /** 内部使用：代理调用后清除 */
    public static void clearCurrentProxy() {
        currentProxy.remove();
    }

    private AopContext() {}
}
