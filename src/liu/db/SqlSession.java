package liu.db;

import liu.spring.annotation.mybatis.Insert;
import liu.spring.annotation.mybatis.Select;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.sql.*;
import java.util.*;

public class SqlSession {

    // 1. 配置信息（实际项目建议读取 properties 文件）
    private static String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static String URL = "jdbc:mysql://localhost:3306/mini_spring?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true";
    private static String USERNAME = "root";
    private static String PASSWORD = "root";

    // 2. 静态代码块：注册驱动
    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("数据库驱动加载失败: " + e.getMessage());
        }
    }

    /**
     * 核心方法：获取 Mapper 代理对象
     */
    public static <T> T getMapper(Class<T> mapperInterface) {
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[]{mapperInterface},
                new MapperProxy()
        );
    }

    // 3. 内部类：代理处理器
    private static class MapperProxy implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理 Object 的基础方法
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) {
                    return "MapperProxy{" + method.getDeclaringClass().getSimpleName() + "}";
                } else if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                } else if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
                return null;
            }

            // 处理自定义注解
            if (method.isAnnotationPresent(Select.class)) {
                Select select = method.getAnnotation(Select.class);
                return executeQuery(select.value(), args);
            } else if (method.isAnnotationPresent(Insert.class)) {
                Insert insert = method.getAnnotation(Insert.class);
                return executeUpdate(insert.value(), args);
            }

            throw new RuntimeException("不支持的注解或方法: " + method.getName());
        }

        // 查询逻辑
        private Object executeQuery(String sql, Object[] args) {
            try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // 设置参数
                setParameters(stmt, args);

                try (ResultSet rs = stmt.executeQuery()) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        result.add(row);
                    }
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

        // 更新逻辑
        private int executeUpdate(String sql, Object[] args) {
            try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, args);
                return stmt.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        // 参数设置辅助方法
        private void setParameters(PreparedStatement stmt, Object[] args) throws SQLException {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    stmt.setObject(i + 1, args[i]);
                }
            }
        }
    }
}