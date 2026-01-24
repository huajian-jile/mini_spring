//package liu.Advice;
//
//import net.sf.cglib.proxy.Enhancer;
//import net.sf.cglib.proxy.MethodInterceptor;
//import net.sf.cglib.proxy.MethodProxy;
//
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//
//public class ProxyFactory {
//
//    /**
//     * 自动创建代理对象
//     * @param target 目标对象
//     * @param advice 增强逻辑
//     * @return 代理对象
//     */
//    public static Object createProxy(Object target, Advice advice) {
//
//        Class<?> targetClass = target.getClass();
//
//        // --- 第一步：判断是否需要使用 CGLIB ---
//        // 规则1：如果该类没有实现任何接口，必须用 CGLIB
//        // 规则2：如果该类是 JDK 动态代理生成的类（通常包含 Proxy），用 JDK 代理
//        // 规则3：通常我们优先使用 JDK 代理，如果没有接口则用 CGLIB
//
//        if (targetClass.getInterfaces().length == 0) {
//            // 没有实现接口，强制使用 CGLIB
//            return createCglibProxy(target, advice);
//        } else {
//            // 有接口，优先使用 JDK 动态代理
//            return createJdkProxy(target, advice);
//        }
//    }
//
//    // --- JDK 动态代理实现 ---
//    private static Object createJdkProxy(Object target, Advice advice) {
//        return Proxy.newProxyInstance(
//            target.getClass().getClassLoader(),
//            target.getClass().getInterfaces(),
//            new InvocationHandler() {
//                @Override
//                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                    // 调用通用的增强逻辑
//                    return advice.invoke(method, args, target);
//                }
//            }
//        );
//    }
//
//    // --- CGLIB 动态代理实现 ---
//    private static Object createCglibProxy(Object target, Advice advice) {
//        Enhancer enhancer = new Enhancer();
//        enhancer.setSuperclass(target.getClass());
//        enhancer.setCallback(new MethodInterceptor() {
//            @Override
//            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
//                // 这里需要区分：如果是 CGLIB 代理，target 就是父类实例
//                return advice.invoke(method, args, target);
//            }
//        });
//        return enhancer.create();
//    }
//}