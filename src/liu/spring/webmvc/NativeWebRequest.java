package liu.spring.webmvc;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 对 HttpExchange 的简单封装，提供路径、方法、查询参数、请求体等（类似 ServletRequest）。
 */
public class NativeWebRequest {

    private final HttpExchange exchange;
    private final String path;
    private final String method;
    private final Map<String, String> queryParams;
    private final Map<String, String> pathVariables;
    private byte[] bodyBytes;

    public NativeWebRequest(HttpExchange exchange) {
        this.exchange = exchange;
        URI uri = exchange.getRequestURI();
        this.path = uri.getPath();
        this.method = exchange.getRequestMethod() != null ? exchange.getRequestMethod().toUpperCase() : "GET";
        this.queryParams = parseQuery(uri.getQuery());
        this.pathVariables = new HashMap<>();
    }

    public HttpExchange getExchange() { return exchange; }
    public String getPath() { return path; }
    public String getMethod() { return method; }
    public Map<String, String> getPathVariables() { return pathVariables; }
    public void setPathVariable(String name, String value) { pathVariables.put(name, value); }

    public String getParameter(String name) {
        return queryParams.get(name);
    }

    public String getParameter(String name, String defaultValue) {
        String v = queryParams.get(name);
        return v != null ? v : defaultValue;
    }

    /** 读取请求体为字符串（会缓存，多次调用不重复读） */
    public String getBodyAsString() throws IOException {
        if (bodyBytes == null) {
            InputStream is = exchange.getRequestBody();
            if (is == null) return "";
            try (Scanner s = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
                String str = s.hasNext() ? s.next() : "";
                bodyBytes = str.getBytes(StandardCharsets.UTF_8);
                return str;
            }
        }
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    public byte[] getBodyAsBytes() throws IOException {
        if (bodyBytes == null) getBodyAsString();
        return bodyBytes != null ? bodyBytes : new byte[0];
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                map.put(decode(pair.substring(0, eq).trim()), decode(pair.substring(eq + 1).trim()));
            } else {
                map.put(decode(pair.trim()), "");
            }
        }
        return map;
    }

    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }
}
