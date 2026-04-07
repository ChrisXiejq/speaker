package com.speakingkiller.app.model.entity;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private String wechatOpenid;
    private Instant createdAt;
}
