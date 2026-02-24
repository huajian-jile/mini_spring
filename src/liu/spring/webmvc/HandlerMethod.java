package liu.spring.webmvc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 封装控制器方法 + 路径模式（支持 /user/{id}），与 Spring 的 HandlerMethod 对应。
 */
public class HandlerMethod {

    private final Object bean;
    private final Method method;
    /** 映射路径模式，如 /user/list 或 /user/{id} */
    private final String pattern;
    /** 请求方法限制，空表示不限制 */
    private final String[] httpMethods;
    /** 路径中的变量名顺序，如 ["id"] for /user/{id} */
    private final List<String> pathVariableNames;
    /** 编译后的正则，用于匹配并提取 path variable */
    private final Pattern compiledPattern;

    public HandlerMethod(Object bean, Method method, String pattern, String[] httpMethods) {
        this.bean = bean;
        this.method = method;
        this.pattern = pattern;
        this.httpMethods = httpMethods != null ? httpMethods : new String[0];
        this.pathVariableNames = new ArrayList<>();
        this.compiledPattern = compilePattern(pattern, pathVariableNames);
    }

    public Object getBean() { return bean; }
    public Method getMethod() { return method; }
    public String getPattern() { return pattern; }
    public String[] getHttpMethods() { return httpMethods; }
    public List<String> getPathVariableNames() { return pathVariableNames; }
    public Pattern getCompiledPattern() { return compiledPattern; }

    /**
     * 将 /user/{id} 转为正则，并把变量名放入 pathVariableNames。
     */
    private static Pattern compilePattern(String pattern, List<String> pathVariableNames) {
        if (pattern == null || pattern.isEmpty()) return Pattern.compile("^$");
        StringBuilder regex = new StringBuilder("^");
        int i = 0;
        while (i < pattern.length()) {
            if (pattern.charAt(i) == '{') {
                int end = pattern.indexOf('}', i);
                if (end > i) {
                    String varName = pattern.substring(i + 1, end).trim();
                    pathVariableNames.add(varName);
                    regex.append("([^/]+)");
                    i = end + 1;
                    continue;
                }
            }
            char c = pattern.charAt(i);
            if (c == '*' || c == '.' || c == '?' || c == '[' || c == ']' || c == '(' || c == ')' || c == '\\' || c == '+') {
                regex.append('\\');
            }
            regex.append(c);
            i++;
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }

    /** 用请求路径匹配此 handler，若匹配则把路径变量写入 request 并返回 true */
    public boolean matchAndExtract(String path, String requestMethod, NativeWebRequest request) {
        if (path == null) return false;
        if (httpMethods.length > 0) {
            boolean methodOk = false;
            for (String m : httpMethods) {
                if (m != null && m.equalsIgnoreCase(requestMethod)) { methodOk = true; break; }
            }
            if (!methodOk) return false;
        }
        java.util.regex.Matcher matcher = compiledPattern.matcher(path);
        if (!matcher.matches()) return false;
        for (int i = 0; i < pathVariableNames.size() && i < matcher.groupCount(); i++) {
            request.setPathVariable(pathVariableNames.get(i), matcher.group(i + 1));
        }
        return true;
    }
}
