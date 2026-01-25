package liu.container;

import java.util.regex.Pattern;

/**
 * 切点表达式匹配器
 * 支持多种表达式格式的匹配
 */
public class PointcutMatcher {
    
    /**
     * 判断类是否匹配切点表达式
     * 
     * @param expression 切点表达式
     * @param targetClass 目标类
     * @return 是否匹配
     */
    public static boolean matches(String expression, Class<?> targetClass) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }
        
        String className = targetClass.getName();
        
        // 1. 注解匹配：@Log 或 @ExecutionTime
        if (expression.startsWith("@")) {
            // 这种情况在方法级别判断，类级别返回 false
            return false;
        }
        
        // 2. execution 表达式：execution(* liu.service.*.*(..))
        if (expression.startsWith("execution(")) {
            return matchesExecution(expression, className);
        }
        
        // 3. 精确匹配：liu.service.UserService
        if (className.equals(expression)) {
            return true;
        }
        
        // 4. 包路径递归通配：liu.service..**
        if (expression.endsWith("..**")) {
            String packagePrefix = expression.substring(0, expression.length() - 3);
            return className.startsWith(packagePrefix);
        }
        
        // 5. 包路径通配：liu.service.*
        if (expression.endsWith(".*")) {
            String packagePrefix = expression.substring(0, expression.length() - 1);
            // 匹配包路径，但不包含子包
            if (className.startsWith(packagePrefix)) {
                String rest = className.substring(packagePrefix.length());
                // 确保没有更多的点（即不是子包）
                return !rest.contains(".");
            }
            return false;
        }
        
        // 6. 类名通配：*Service
        if (expression.contains("*")) {
            String regex = expression
                .replace(".", "\\.")
                .replace("*", ".*");
            return Pattern.matches(regex, className);
        }
        
        return false;
    }
    
    /**
     * 判断方法是否匹配注解表达式
     * 
     * @param expression 注解表达式（如 @Log）
     * @param method 目标方法
     * @return 是否匹配
     */
    public static boolean matchesAnnotation(String expression, java.lang.reflect.Method method) {
        if (!expression.startsWith("@")) {
            return false;
        }
        
        String annotationName = expression.substring(1);
        
        // 检查方法上的注解
        for (java.lang.annotation.Annotation annotation : method.getAnnotations()) {
            String simpleAnnotationName = annotation.annotationType().getSimpleName();
            if (simpleAnnotationName.equals(annotationName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 匹配 execution 表达式（简化版）
     * 格式：execution(* liu.service.*.*(..))
     */
    private static boolean matchesExecution(String expression, String className) {
        // 移除 execution( 和 )
        String content = expression.substring(10, expression.length() - 1).trim();
        
        // 简化处理：提取包路径部分
        // execution(* liu.service.*.*(..)) -> liu.service.*
        String[] parts = content.split("\\s+");
        if (parts.length >= 2) {
            String methodPattern = parts[1];
            int lastDot = methodPattern.lastIndexOf('.');
            if (lastDot > 0) {
                String classPattern = methodPattern.substring(0, lastDot);
                // 递归调用 matches，传入 className 字符串比较
                return matchesClassPattern(classPattern, className);
            }
        }
        
        return false;
    }
    
    /**
     * 匹配类名模式
     */
    private static boolean matchesClassPattern(String pattern, String className) {
        // 精确匹配
        if (className.equals(pattern)) {
            return true;
        }
        
        // 包路径递归通配
        if (pattern.endsWith("..**")) {
            String packagePrefix = pattern.substring(0, pattern.length() - 3);
            return className.startsWith(packagePrefix);
        }
        
        // 包路径通配
        if (pattern.endsWith(".*")) {
            String packagePrefix = pattern.substring(0, pattern.length() - 1);
            if (className.startsWith(packagePrefix)) {
                String rest = className.substring(packagePrefix.length());
                return !rest.contains(".");
            }
            return false;
        }
        
        // 通配符匹配
        if (pattern.contains("*")) {
            String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
            return Pattern.matches(regex, className);
        }
        
        return false;
    }
}

