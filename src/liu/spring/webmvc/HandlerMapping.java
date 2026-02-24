package liu.spring.webmvc;

/**
 * 处理器映射接口：根据请求查找 Handler 及执行链（与 Spring 的 HandlerMapping 一致）。
 */
public interface HandlerMapping {

    /**
     * 根据请求返回执行链，找不到则返回 null。
     */
    HandlerExecutionChain getHandler(NativeWebRequest request);
}
