package liu.util;

import java.lang.reflect.Method;

// 封装处理器信息
public class Handler {
    public Object controller; // 控制器实例
    public Method method;     // 要调用的方法
    public String url;        // 请求路径

    public Handler(Object controller, Method method, String url) {
        this.controller = controller;
        this.method = method;
        this.url = url;
    }
}