package liu;

import liu.annotation.spring.ioc.Autowired;
import liu.container.MyApplicationContext;
import liu.annotation.spring.ioc.SpringBootApplication;
import liu.mapper.UserMapper;
import liu.service.IUserService;
import liu.service.UserServiceImp;
import liu.util.MySpringApplication;

import java.util.List;
import java.util.Map;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
@SpringBootApplication
public class Application {
    public static void main(String[] args) throws Exception {

        MyApplicationContext context = MySpringApplication.run(Application.class);
        System.out.println("项目启动成功");
        IUserService userServiceImp =(IUserService) context.getBean("userServiceImp");
        userServiceImp.getAllUsers();

    }
}