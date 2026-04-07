package com.speakingkiller.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient dashScopeRestClient() {
        return RestClient.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
    }

    /** 百炼原生 HTTP（CosyVoice 等），与 OpenAI 兼容基座分离 */
    @Bean
    public RestClient dashScopeApiRestClient() {
        return RestClient.builder()
                .baseUrl("https://dashscope.aliyuncs.com/api/v1")
                .build();
    }

}
