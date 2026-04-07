package com.speaker.app.service;

import com.speaker.app.mapper.UserMapper;
import com.speaker.app.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private final UserMapper userMapper;

    public CurrentUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User requireCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userMapper.findByUsername(username);
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        return u;
    }
}
