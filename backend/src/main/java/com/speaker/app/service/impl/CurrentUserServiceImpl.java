package com.speaker.app.service.impl;

import com.speaker.app.repository.UserMapper;
import com.speaker.app.model.entity.User;
import com.speaker.app.service.intf.CurrentUserServiceIntf;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserServiceImpl implements CurrentUserServiceIntf {

    private static final String DEFAULT_USERNAME = "default-user";

    private final UserMapper userMapper;

    public CurrentUserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public synchronized User requireCurrentUser() {
        User user = userMapper.findFirstById();
        if (user != null) {
            return user;
        }

        user = User.builder()
                .username(DEFAULT_USERNAME)
                .createdAt(System.currentTimeMillis())
                .build();
        userMapper.insert(user);
        return user;
    }
}
