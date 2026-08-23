package com.speaker.app.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speaker.app.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users ORDER BY id ASC LIMIT 1")
    User findFirstById();
}
