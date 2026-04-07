package com.speakingkiller.app.mapper;

import com.speakingkiller.app.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    int insert(User user);

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);
}
