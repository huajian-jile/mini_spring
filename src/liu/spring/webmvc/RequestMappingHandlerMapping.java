package liu.spring.webmvc;

import liu.spring.annotation.spring.ioc.Controller;
import liu.spring.annotation.web.GetMapping;
import liu.spring.annotation.web.PostMapping;
import liu.spring.annotation.web.RequestMapping;
import liu.spring.annotation.web.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 @RequestMapping / @GetMapping / @PostMapping 的处理器映射，与 Spring 的 RequestMappingHandlerMapping 一致。
 * 支持路径变量，如 /user/{id}。
 */
public class RequestMappingHandlerMapping implements HandlerMapping {

    /** 精确路径 -> 该路径下各方法的 HandlerMethod（同一路径可有 GET/POST 等不同方法） */
    private final Map<String, List<HandlerMethod>> exactLookup = new ConcurrentHashMap<>();
    /** 带路径变量的 HandlerMethod，按注册顺序匹配 */
    private final List<HandlerMethod> patternHandlers = new ArrayList<>();

    /**
     * 注册控制器：扫描类上的 @RequestMapping 及方法上的 @RequestMapping / @GetMapping / @PostMapping。
     */
    public void registerHandler(Object controller, Class<?> controllerClass) {
        String classPath = "";
        String[] classMethods = null;
        if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = controllerClass.getAnnotation(RequestMapping.class);
            classPath = rm.value() != null ? rm.value() : "";
            classMethods = rm.method().length > 0 ? rm.method() : null;
        }
        for (Method method : controllerClass.getDeclaredMethods()) {
            String methodPath = null;
            String[] methods = classMethods;
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping rm = method.getAnnotation(RequestMapping.class);
                methodPath = rm.value();
                if (rm.method().length > 0) methods = rm.method();
            } else if (method.isAnnotationPresent(GetMapping.class)) {
                methodPath = method.getAnnotation(GetMapping.class).value();
                methods = new String[]{"GET"};
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                methodPath = method.getAnnotation(PostMapping.class).value();
                methods = new String[]{"POST"};
            }
            if (methodPath == null) continue;
            String fullPath = ("/" + classPath + "/" + methodPath).replaceAll("/+", "/");
            if (fullPath.length() > 1 && fullPath.endsWith("/")) fullPath = fullPath.substring(0, fullPath.length() - 1);
            HandlerMethod handlerMethod = new HandlerMethod(controller, method, fullPath, methods);
            if (fullPath.contains("{") && fullPath.contains("}")) {
                patternHandlers.add(handlerMethod);
            } else {
                exactLookup.computeIfAbsent(fullPath, k -> new ArrayList<>()).add(handlerMethod);
            }
        }
    }

    @Override
    public HandlerExecutionChain getHandler(NativeWebRequest request) {
        String path = request.getPath();
        String method = request.getMethod();
        List<HandlerMethod> list = exactLookup.get(path);
        if (list != null) {
            for (HandlerMethod hm : list) {
                if (matchMethod(hm, method)) return new HandlerExecutionChain(hm);
            }
        }
        for (HandlerMethod patternHandler : patternHandlers) {
            if (patternHandler.matchAndExtract(path, method, request)) {
                return new HandlerExecutionChain(patternHandler);
            }
        }
        return null;
    }

    private static boolean matchMethod(HandlerMethod hm, String requestMethod) {
        String[] allowed = hm.getHttpMethods();
        if (allowed == null || allowed.length == 0) return true;
        for (String m : allowed) {
            if (m != null && m.equalsIgnoreCase(requestMethod)) return true;
        }
        return false;
    }

    public Map<String, List<HandlerMethod>> getExactLookup() { return exactLookup; }
    public List<HandlerMethod> getPatternHandlers() { return patternHandlers; }
}
