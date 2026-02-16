package liu.util;

import liu.spring.ioc.MyApplicationContext;
import liu.spring.annotation.spring.ioc.SpringBootApplication;

public class MySpringApplication {

    /**
     * 一行启动！模仿 Spring Boot
     */
    public static MyApplicationContext run(Class<?> appClass, String... args) {
        // 检查注解
        if (!appClass.isAnnotationPresent(SpringBootApplication.class)) {
            throw new RuntimeException("启动类必须包含 @SpringBootApplication 注解");
        }

        // 创建上下文并刷新
        MyApplicationContext context = new MyApplicationContext();
        try {
            context.refresh(appClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return context;
    }
}