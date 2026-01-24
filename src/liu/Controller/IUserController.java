package liu.Controller;

import liu.annotation.spring.aop.ExecutionTime;
import liu.annotation.spring.aop.Log;
import liu.annotation.web.GetMapping;
import liu.annotation.web.PostMapping;
import liu.annotation.web.RequestMapping;

import java.util.List;
import java.util.Map;

public interface IUserController {

    public List<Map<String, Object>> listUsers();

//    public String createUser();
//
//    public String testSlowOperation() ;
//
//    public String index() ;
}
