package com.speakingkiller.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.speakingkiller.app.mapper")
public class SpeakingKillerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeakingKillerApplication.class, args);
    }
}
