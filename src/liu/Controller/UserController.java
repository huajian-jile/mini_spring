package liu.Controller;

import liu.Application;
import liu.annotation.spring.aop.ExecutionTime;
import liu.annotation.spring.aop.Log;
import liu.annotation.spring.ioc.Autowired;
import liu.annotation.spring.ioc.Component;
import liu.annotation.web.GetMapping;
import liu.annotation.web.PostMapping;
import liu.annotation.web.RequestMapping;
import liu.annotation.web.RestController;
import liu.container.MyApplicationContext;
import liu.service.IUserService;
import liu.service.UserServiceImp;
import liu.util.MySpringApplication;

import java.util.List;
import java.util.Map;



@Component
@RestController // 或者 @Controller
@RequestMapping("/user") // 类级别的路径
public class UserController {
    @Autowired
    public IUserService userService;

    // GET http://localhost:8080/user/list
    @GetMapping("/list")
    @Log(value = "查询用户列表接口", printArgs = false, printResult = true)
    @ExecutionTime(value = "用户列表查询", threshold = 0)
    public List<Map<String, Object>> listUsers() {
        System.out.println("Controller: 开始处理用户列表请求");
        List<Map<String, Object>> allUsers = userService.getAllUsers();
        return allUsers;
    }

    // POST http://localhost:8080/user/create
    @PostMapping("/create")
    @Log("创建用户接口")
    public String createUser() {
        return "用户创建成功";
    }

    // GET http://localhost:8080/user/slow
    // 测试慢速操作和执行时间阈值
    @GetMapping("/slow")
    @ExecutionTime(value = "慢速接口", threshold = 100)
    public String testSlowOperation() {
        return userService.slowOperation();
    }

    // 如果不指定 value，默认映射到 /user
    @RequestMapping
    public String index() {
        return "Welcome to Mini Spring with AOP!";
    }
}