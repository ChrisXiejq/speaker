package com.speakingkiller.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakingkiller.app.config.AppProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QwenService {

    private final RestClient dashScopeRestClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public QwenService(
            @Qualifier("dashScopeRestClient") RestClient dashScopeRestClient,
            AppProperties appProperties,
            ObjectMapper objectMapper) {
        this.dashScopeRestClient = dashScopeRestClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public String chat(String systemPrompt, List<Map<String, String>> messages) {
        String apiKey = appProperties.getDashscope().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 DASHSCOPE_API_KEY，无法调用通义千问");
        }
        String model = appProperties.getDashscope().getChatModel();
        List<Map<String, String>> full = new ArrayList<>();
        full.add(Map.of("role", "system", "content", systemPrompt));
        full.addAll(messages);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", full);

        String raw = dashScopeRestClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new IllegalStateException("模型返回格式异常");
            }
            return content.asText();
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("解析模型响应失败", e);
        }
    }
}
