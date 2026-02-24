package liu.spring.webmvc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 前端控制器：根据请求查找 Handler，交给 HandlerAdapter 执行，与 Spring 的 DispatcherServlet 一致。
 */
public class DispatcherServlet implements HttpHandler {

    private final List<HandlerMapping> handlerMappings;
    private final List<HandlerAdapter> handlerAdapters;

    public DispatcherServlet(List<HandlerMapping> handlerMappings, List<HandlerAdapter> handlerAdapters) {
        this.handlerMappings = handlerMappings != null ? handlerMappings : new java.util.ArrayList<>();
        this.handlerAdapters = handlerAdapters != null ? handlerAdapters : new java.util.ArrayList<>();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        NativeWebRequest request = new NativeWebRequest(exchange);
        try {
            HandlerExecutionChain chain = getHandler(request);
            if (chain == null) {
                sendError(exchange, 404, "Not Found");
                return;
            }
            Object handler = chain.getHandler();
            HandlerAdapter adapter = getHandlerAdapter(handler);
            if (adapter == null) {
                sendError(exchange, 500, "No adapter for handler: " + handler.getClass().getName());
                return;
            }
            ModelAndView mv = adapter.handle(request, handler);
            if (mv != null && mv.getViewName() != null) {
                sendError(exchange, 500, "View resolution not implemented, viewName: " + mv.getViewName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, e.getMessage() != null ? e.getMessage() : "Internal Server Error");
        } finally {
            exchange.close();
        }
    }

    protected HandlerExecutionChain getHandler(NativeWebRequest request) {
        for (HandlerMapping mapping : handlerMappings) {
            HandlerExecutionChain chain = mapping.getHandler(request);
            if (chain != null) return chain;
        }
        return null;
    }

    protected HandlerAdapter getHandlerAdapter(Object handler) {
        for (HandlerAdapter adapter : handlerAdapters) {
            if (adapter.supports(handler)) return adapter;
        }
        return null;
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] bytes = (message != null ? message : "").getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
