package liu.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataSource {

    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName;



    public MyDataSource(String driverClassName, String jdbcUrl, String username, String password) {
        //this.driverClassName = driverClassName;
        this.jdbcUrl = jdbcUrl;
        this.username = "root";
        this.password = "root";
        // 注册驱动
//        try {
//            Class.forName(driverClassName);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException("找不到JDBC驱动: " + driverClassName, e);
//        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}