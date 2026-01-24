package liu.mapper;



import liu.annotation.mybatis.Insert;
import liu.annotation.mybatis.Select;
import liu.annotation.spring.ioc.Mapper;

import java.util.List;
import java.util.Map;

// 注意：依然需要 @Repository，让 Spring 容器知道这是一个 Bean
@Mapper
public interface UserMapper {

    // 查询所有
    @Select("SELECT * FROM textmybatis")
    List<Map<String, Object>> findAll();

    // 根据 ID 查询
    @Select("SELECT * FROM users WHERE id = #{id}")
    Map<String, Object> findById(int id);

    // 插入
    @Insert("INSERT INTO users(name, email) VALUES(#{name}, #{email})")
    int insert(String name, String email);
}