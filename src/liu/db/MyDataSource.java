package liu.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.sql.Connection;
import java.sql.SQLException;

public class MyDataSource {

    private String jdbcUrl;
    private String username;
    private String password;

    // 修复：使用传入的参数，不再写死
    public MyDataSource(String driverClassName, String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;

        // 注册驱动
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("找不到JDBC驱动: " + driverClassName, e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}