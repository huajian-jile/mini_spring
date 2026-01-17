package liu.service;

import java.util.List;
import java.util.Map;

public interface IUserService {
     List<Map<String, Object>> getAllUsers();
     public String slowOperation() ;
}
