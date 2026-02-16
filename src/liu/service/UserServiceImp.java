package liu.service;

import liu.spring.annotation.spring.ioc.Autowired;
import liu.spring.annotation.spring.ioc.Component;
import liu.mapper.UserMapper;
import liu.spring.annotation.spring.aop.Log;
import liu.spring.annotation.spring.aop.ExecutionTime;
import liu.spring.annotation.spring.ioc.Service;

import java.util.List;
import java.util.Map;

@Service
@Component
public class UserServiceImp implements IUserService {


    @Autowired
    private UserMapper userMapper; // 直接注入接口！


    /**
     * //    @Log("日志打印：数据库开始查询")
     * 从数据库获取所有用户（只记录执行时间）
     */
    @Override
    @Log("日志打印：数据库开始查询")
    @ExecutionTime(value = "数据库查询用户", threshold = 50)
    public List<Map<String, Object>> getAllUsers() {
        return userMapper.findAll();
    }

    /**
     * //    @Log("执行慢速操作")
     * 模拟一个慢速操作（用于测试执行时间阈值）
     */
    @Override
    @ExecutionTime(value = "慢速操作", threshold = 100)
    public String slowOperation() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "慢速操作完成";
    }

}