package liu.spring.webmvc;

import liu.spring.annotation.web.RequestBody;
import liu.spring.annotation.web.RequestParam;
import liu.spring.annotation.web.PathVariable;
import liu.spring.annotation.web.ResponseBody;
import liu.spring.annotation.web.RestController;

import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 适配 HandlerMethod：解析 @RequestParam、@PathVariable、@RequestBody，调用方法，处理 @ResponseBody 返回值（写 JSON）。
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter {

    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

    @Override
    public boolean supports(Object handler) {
        return handler instanceof HandlerMethod;
    }

    @Override
    public ModelAndView handle(NativeWebRequest request, Object handler) throws Exception {
        HandlerMethod hm = (HandlerMethod) handler;
        Object[] args = resolveMethodArguments(hm, request);
        Object returnValue = hm.getMethod().invoke(hm.getBean(), args);
        writeResponse(request.getExchange(), hm, returnValue);
        return null;
    }

    private Object[] resolveMethodArguments(HandlerMethod hm, NativeWebRequest request) throws Exception {
        Method method = hm.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            if (p.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = p.getAnnotation(RequestParam.class);
                String name = nameOrValue(rp.value(), rp.name());
                if (name.isEmpty()) name = p.getName();
                String val = request.getParameter(name);
                if (val == null) val = rp.defaultValue();
                if (val == null && rp.required()) throw new IllegalArgumentException("Missing request param: " + name);
                args[i] = convertTo(val, p.getType());
            } else if (p.isAnnotationPresent(PathVariable.class)) {
                PathVariable pv = p.getAnnotation(PathVariable.class);
                String name = nameOrValue(pv.value(), pv.name());
                if (name.isEmpty()) name = p.getName();
                String val = request.getPathVariables().get(name);
                if (val == null) throw new IllegalArgumentException("Missing path variable: " + name);
                args[i] = convertTo(val, p.getType());
            } else if (p.isAnnotationPresent(RequestBody.class)) {
                String body = request.getBodyAsString();
                if (body == null || body.trim().isEmpty()) {
                    args[i] = null;
                } else {
                    args[i] = parseRequestBody(body, p.getType());
                }
            } else if (p.getType() == NativeWebRequest.class || p.getType() == HttpExchange.class) {
                if (p.getType() == NativeWebRequest.class) args[i] = request;
                else args[i] = request.getExchange();
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private static String nameOrValue(String value, String name) {
        if (value != null && !value.isEmpty()) return value;
        return name != null ? name : "";
    }

    private static Object convertTo(String val, Class<?> type) {
        if (val == null) return null;
        if (type == String.class) return val;
        if (type == int.class || type == Integer.class) return Integer.parseInt(val);
        if (type == long.class || type == Long.class) return Long.parseLong(val);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(val);
        if (type == double.class || type == Double.class) return Double.parseDouble(val);
        return val;
    }

    /** 简单请求体解析：仅支持 Map/List 的 JSON 或 String；无 Jackson 时用简单解析 */
    private static Object parseRequestBody(String body, Class<?> type) {
        body = body.trim();
        if (type == String.class) return body;
        if (body.startsWith("{") && body.endsWith("}") && type == Map.class) {
            return JsonUtils.parseObject(body);
        }
        if (body.startsWith("[") && body.endsWith("]") && type == List.class) {
            return JsonUtils.parseArray(body);
        }
        return body;
    }

    private void writeResponse(HttpExchange exchange, HandlerMethod hm, Object returnValue) throws Exception {
        if (returnValue == null) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        boolean writeJson = isResponseBody(hm);
        String response;
        if (writeJson) {
            response = returnValue instanceof String && !isJsonLike((String) returnValue)
                    ? JsonUtils.toJson(returnValue)
                    : (returnValue instanceof String ? (String) returnValue : JsonUtils.toJson(returnValue));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_JSON);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            response = returnValue.toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static boolean isResponseBody(HandlerMethod hm) {
        if (hm.getMethod().isAnnotationPresent(ResponseBody.class)) return true;
        if (hm.getBean().getClass().isAnnotationPresent(RestController.class)) return true;
        return false;
    }

    private static boolean isJsonLike(String s) {
        String t = s.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }
}
