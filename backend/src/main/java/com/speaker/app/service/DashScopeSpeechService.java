package com.speaker.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speaker.app.config.AppProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 语音识别（Qwen3-ASR-Flash，OpenAI 兼容）与语音合成（CosyVoice HTTP）。
 * 模型名可在 {@link AppProperties.Dashscope} 中配置。
 */
@Service
public class DashScopeSpeechService {

    private final RestClient dashScopeCompatibleClient;
    private final RestClient dashScopeApiClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public DashScopeSpeechService(
            @Qualifier("dashScopeRestClient") RestClient dashScopeCompatibleClient,
            @Qualifier("dashScopeApiRestClient") RestClient dashScopeApiClient,
            AppProperties appProperties,
            ObjectMapper objectMapper) {
        this.dashScopeCompatibleClient = dashScopeCompatibleClient;
        this.dashScopeApiClient = dashScopeApiClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 将上传的音频转写为英文文本（建议 mp3/wav/webm，体积编码后不超过约 10MB）。
     */
    public String transcribeEnglish(byte[] audioBytes, String filenameHint) {
        String apiKey = appProperties.getDashscope().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 DASHSCOPE_API_KEY");
        }
        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException("音频为空");
        }
        String model = appProperties.getDashscope().getAsrModel();
        String mime = mimeFromFilename(filenameHint);
        String b64 = Base64.getEncoder().encodeToString(audioBytes);
        String dataUri = "data:" + mime + ";base64," + b64;

        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> inputAudio = new HashMap<>();
        inputAudio.put("data", dataUri);
        Map<String, Object> part = new HashMap<>();
        part.put("type", "input_audio");
        part.put("input_audio", inputAudio);
        content.add(part);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", content);

        List<Map<String, Object>> messages = List.of(userMsg);

        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("language", "en");
        asrOptions.put("enable_itn", true);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        body.put("asr_options", asrOptions);

        String raw = dashScopeCompatibleClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return extractChatText(raw);
    }

    private static String mimeFromFilename(String filenameHint) {
        if (filenameHint == null) {
            return "audio/mpeg";
        }
        String lower = filenameHint.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        if (lower.endsWith(".webm")) {
            return "audio/webm";
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".m4a")) {
            return "audio/mp4";
        }
        if (lower.endsWith(".ogg")) {
            return "audio/ogg";
        }
        return "audio/mpeg";
    }

    private String extractChatText(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new IllegalStateException("ASR 返回格式异常: " + truncate(raw));
            }
            String text = content.asText("").trim();
            if (text.isEmpty()) {
                throw new IllegalStateException("未识别到有效英文内容");
            }
            return text;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析 ASR 响应失败", e);
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }

    /**
     * 英文朗读，返回 MP3/WAV 等二进制（与 {@link AppProperties.Dashscope#getTtsFormat()} 一致）。
     */
    public byte[] synthesizeEnglish(String text) {
        String apiKey = appProperties.getDashscope().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 DASHSCOPE_API_KEY");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本为空");
        }
        int max = appProperties.getDashscope().getTtsMaxChars();
        if (text.length() > max) {
            throw new IllegalArgumentException("文本过长，请控制在 " + max + " 字以内");
        }

        String model = appProperties.getDashscope().getTtsModel();
        String voice = appProperties.getDashscope().getTtsVoice();
        String format = appProperties.getDashscope().getTtsFormat();
        int sampleRate = appProperties.getDashscope().getTtsSampleRate();

        Map<String, Object> input = new HashMap<>();
        input.put("text", text);
        input.put("voice", voice);
        input.put("format", format);
        input.put("sample_rate", sampleRate);
        input.put("language_hints", List.of("en"));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", input);

        String raw = dashScopeApiClient.post()
                .uri("/services/audio/tts/SpeechSynthesizer")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode audio = root.path("output").path("audio");
            String url = audio.path("url").asText(null);
            if (url != null && !url.isBlank()) {
                return RestClient.create().get().uri(url).retrieve().body(byte[].class);
            }
            String data = audio.path("data").asText(null);
            if (data != null && !data.isBlank()) {
                return Base64.getDecoder().decode(data);
            }
            throw new IllegalStateException("TTS 返回无音频: " + truncate(raw));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析或拉取 TTS 音频失败", e);
        }
    }
}
