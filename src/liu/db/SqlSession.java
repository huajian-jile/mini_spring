package liu.db;

import liu.annotation.mybatis.Insert;
import liu.annotation.mybatis.Select;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlSession {

//    // 硬编码的数据库连接信息（你可以改成读配置文件）
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String URL = "jdbc:mysql://localhost:3306/big_event?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
//
    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据接口获取代理对象
     */
    public static <T> T getMapper(Class<T> mapperInterface) {
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[]{mapperInterface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // 1. 先处理 Object 的基础方法，防止死循环
                        if (method.getDeclaringClass() == Object.class) {
                            // 如果是 toString, equals, hashCode 等方法，直接处理或返回默认值
                            if ("toString".equals(method.getName())) {
                                return "MapperProxy{" + mapperInterface.getSimpleName() + "}";
                            }
                            // 其他 Object 方法（如 hashCode, equals）也可以在这里处理
                            return null; // 或者根据需要返回默认值
                        }

                        // ✅ 新增：特殊处理 toString 方法
                        if ("toString".equals(method.getName())) {
                            return String.format("SqlSession.MapperProxy{interface=%s}", mapperInterface.getSimpleName());
                        }


                        String sql = null;

                        // 解析方法上的注解
                        if (method.isAnnotationPresent(Select.class)) {
                            sql = method.getAnnotation(Select.class).value();
                            return executeQuery(sql, args);
                        } else if (method.isAnnotationPresent(Insert.class)) {
                            sql = method.getAnnotation(Insert.class).value();
                            return executeUpdate(sql, args);
                        }

                        throw new RuntimeException("不支持的方法：" + method.getName());
                    }

                    private Object executeQuery(String sql, Object[] args) {
                        if (args == null) {
                            args = new Object[]{new Object()}; // 如果是 null，初始化为空数组
                        }
                        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
                            PreparedStatement stmt = conn.prepareStatement(sql);
                            // 👇 关键修改：只有当 args 不为空且 sql 包含 ? 占位符时才设置参数
                            if (args.length > 0 && sql.contains("?")) {
                                for (int i = 0; i < args.length; i++) {
                                    stmt.setObject(i + 1, args[i]); // 注意：这里是 i + 1，因为 JDBC 参数索引从 1 开始
                                }
                            }
                            ResultSet rs = stmt.executeQuery();
                            List<Map<String, Object>> list = new ArrayList<>();
                            ResultSetMetaData metaData = rs.getMetaData();
                            int columnCount = metaData.getColumnCount();

                            while (rs.next()) {
                                Map<String, Object> row = new HashMap<>();
                                for (int i = 1; i <= columnCount; i++) {
                                    row.put(metaData.getColumnName(i), rs.getObject(i));
                                }
                                list.add(row);
                            }
                            for (Map<String, Object> map : list) {
                                // 遍历每个 Map 中的键值对
                                for (String key : map.keySet()) {
                                    Object value = map.get(key);
                                    System.out.println("Key: " + key + ", Value: " + value);
                                }
                            }
                            return list;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    private int executeUpdate(String sql, Object[] args) {
                        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
                            PreparedStatement stmt = conn.prepareStatement(sql);
                            for (int i = 0; i < args.length; i++) {
                                stmt.setObject(i + 1, args[i]);
                            }
                            return stmt.executeUpdate();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return -1;
                        }
                    }
                }
        );
    }
}