package com.speaker.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.speaker.app.mapper")
public class SpeakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeakerApplication.class, args);
    }
}
