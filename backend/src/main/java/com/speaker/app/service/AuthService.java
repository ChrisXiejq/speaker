package com.speaker.app.service;

import com.speaker.app.config.AppProperties;
import com.speaker.app.dto.AuthLoginRequest;
import com.speaker.app.dto.AuthRegisterRequest;
import com.speaker.app.dto.TokenResponse;
import com.speaker.app.mapper.UserMapper;
import com.speaker.app.model.entity.User;
import com.speaker.app.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties appProperties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    public TokenResponse register(AuthRegisterRequest req) {
        if (userMapper.findByUsername(req.username()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = User.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .createdAt(Instant.now())
                .build();
        userMapper.insert(u);
        return issueToken(u.getUsername());
    }

    public TokenResponse login(AuthLoginRequest req) {
        User u = userMapper.findByUsername(req.username());
        if (u == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (u.getPasswordHash() == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return issueToken(u.getUsername());
    }

    private TokenResponse issueToken(String username) {
        String token = jwtService.createToken(username);
        return new TokenResponse(token, "Bearer", appProperties.getJwt().getExpirationMs());
    }
}
