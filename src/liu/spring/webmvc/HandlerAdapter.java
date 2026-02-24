package liu.spring.webmvc;

/**
 * 处理器适配器接口：是否支持某 handler、执行并返回 ModelAndView（与 Spring 的 HandlerAdapter 一致）。
 */
public interface HandlerAdapter {

    /**
     * 是否支持该 handler（如 HandlerMethod）。
     */
    boolean supports(Object handler);

    /**
     * 处理请求，返回 ModelAndView；若直接写响应则返回 null。
     */
    ModelAndView handle(NativeWebRequest request, Object handler) throws Exception;
}
