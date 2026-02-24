package liu.spring.webmvc;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 简单 JSON 序列化/反序列化（无第三方依赖），支持 Map、List、基本类型、String。
 */
public final class JsonUtils {

    /** 简单解析 JSON 对象为 Map（仅支持 key 为字符串，value 为 String/Number/Boolean/Map/List） */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        if (json == null || (json = json.trim()).isEmpty()) return new HashMap<>();
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return new HashMap<>();
        Map<String, Object> out = new LinkedHashMap<>();
        SimpleJsonParser p = new SimpleJsonParser(json.substring(1, json.length() - 1).trim());
        while (!p.eof()) {
            String key = p.readString();
            if (key == null) break;
            p.skip(':');
            Object val = p.readValue();
            out.put(key, val);
            if (!p.skip(',')) break;
        }
        return out;
    }

    /** 简单解析 JSON 数组为 List */
    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        if (json == null || (json = json.trim()).isEmpty()) return new ArrayList<>();
        if (!json.startsWith("[") || !json.endsWith("]")) return new ArrayList<>();
        List<Object> out = new ArrayList<>();
        SimpleJsonParser p = new SimpleJsonParser(json.substring(1, json.length() - 1).trim());
        while (!p.eof()) {
            out.add(p.readValue());
            if (!p.skip(',')) break;
        }
        return out;
    }

    private static class SimpleJsonParser {
        final String s;
        int i;
        SimpleJsonParser(String s) { this.s = s; this.i = 0; }
        boolean eof() { return i >= s.length(); }
        boolean skip(char c) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            if (i < s.length() && s.charAt(i) == c) { i++; return true; }
            return false;
        }
        String readString() {
            while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
            if (i >= s.length() || s.charAt(i) != '"') return null;
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length() && s.charAt(i) != '"') {
                if (s.charAt(i) == '\\') { i++; if (i < s.length()) sb.append(s.charAt(i++)); }
                else sb.append(s.charAt(i++));
            }
            if (i < s.length()) i++;
            return sb.toString();
        }
        Object readValue() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= s.length()) return null;
            char c = s.charAt(i);
            if (c == '"') { i++; StringBuilder sb = new StringBuilder(); while (i < s.length() && s.charAt(i) != '"') { if (s.charAt(i) == '\\') i++; sb.append(s.charAt(i++)); } if (i < s.length()) i++; return sb.toString(); }
            if (c == '{') { int start = i; int depth = 0; do { if (s.charAt(i) == '{') depth++; else if (s.charAt(i) == '}') depth--; i++; } while (i < s.length() && depth > 0); return parseObject(s.substring(start, i)); }
            if (c == '[') { int start = i; int depth = 0; do { if (s.charAt(i) == '[') depth++; else if (s.charAt(i) == ']') depth--; i++; } while (i < s.length() && depth > 0); return parseArray(s.substring(start, i)); }
            if (c == 't' && s.substring(i).startsWith("true")) { i += 4; return Boolean.TRUE; }
            if (c == 'f' && s.substring(i).startsWith("false")) { i += 5; return Boolean.FALSE; }
            if (c == 'n' && s.substring(i).startsWith("null")) { i += 4; return null; }
            if (c == '-' || Character.isDigit(c)) { int start = i; while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '-' || s.charAt(i) == '.')) i++; String num = s.substring(start, i); try { if (num.contains(".")) return Double.parseDouble(num); return Long.parseLong(num); } catch (Exception e) { return num; } }
            return null;
        }
    }

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
        if (obj instanceof List) return listToJson((List<?>) obj);
        if (obj.getClass().isArray()) return arrayToJson(obj);
        return "\"" + escape(obj.toString()) + "\"";
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(String.valueOf(e.getKey()))).append("\":").append(toJson(e.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String arrayToJson(Object arr) {
        int len = Array.getLength(arr);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(Array.get(arr, i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
