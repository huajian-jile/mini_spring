package liu.Controller;

import liu.spring.annotation.spring.aop.ExecutionTime;
import liu.spring.annotation.spring.aop.Log;
import liu.spring.annotation.spring.ioc.Autowired;
import liu.spring.annotation.spring.ioc.Component;
import liu.spring.annotation.web.GetMapping;
import liu.spring.annotation.web.PostMapping;
import liu.spring.annotation.web.RequestMapping;
import liu.spring.annotation.web.RestController;
import liu.service.IUserService;

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
    @Log(value = "查询用户列表接口", printArgs = false, printResult = true)
    @ExecutionTime(value = "用户列表查询", threshold = 0)
    public String createUser() {
        List<Map<String, Object>> allUsers = userService.getAllUsers();
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