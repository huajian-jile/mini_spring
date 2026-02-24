package liu.spring.webmvc;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理器执行链（Handler + 可选拦截器），与 Spring 的 HandlerExecutionChain 一致。
 */
public class HandlerExecutionChain {

    private final Object handler;
    private final List<Object> interceptorList = new ArrayList<>();

    public HandlerExecutionChain(Object handler) {
        this.handler = handler;
    }

    public Object getHandler() { return handler; }
    public List<Object> getInterceptorList() { return interceptorList; }
    public void addInterceptor(Object interceptor) { interceptorList.add(interceptor); }
}
