package com.speakingkiller.app.web;

import com.speakingkiller.app.config.AppProperties;
import com.speakingkiller.app.service.DashScopeSpeechService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/practice")
public class PracticeSpeechController {

    private final DashScopeSpeechService dashScopeSpeechService;
    private final AppProperties appProperties;

    public PracticeSpeechController(DashScopeSpeechService dashScopeSpeechService, AppProperties appProperties) {
        this.dashScopeSpeechService = dashScopeSpeechService;
        this.appProperties = appProperties;
    }

    /**
     * 上传录音，返回英文转写文本（供随后 POST /sessions/{id}/reply 使用）。
     */
    @PostMapping(value = "/asr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> asr(@RequestParam("file") MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取上传文件失败");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio";
        String text = dashScopeSpeechService.transcribeEnglish(bytes, name);
        return Map.of("text", text);
    }

    public record TtsRequest(String text) {}

    /**
     * 服务端 TTS（MP3/WAV 等），便于小程序下载播放；Web 端也可优先用浏览器 speechSynthesis。
     */
    @PostMapping(value = "/tts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> tts(@RequestBody TtsRequest req) {
        byte[] audio = dashScopeSpeechService.synthesizeEnglish(req.text());
        MediaType mt = ttsContentType(appProperties.getDashscope().getTtsFormat());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mt.toString())
                .body(audio);
    }

    private static MediaType ttsContentType(String format) {
        if (format == null) {
            return MediaType.parseMediaType("audio/mpeg");
        }
        return switch (format.toLowerCase()) {
            case "wav" -> MediaType.parseMediaType("audio/wav");
            case "pcm" -> MediaType.parseMediaType("audio/pcm");
            case "opus" -> MediaType.parseMediaType("audio/opus");
            default -> MediaType.parseMediaType("audio/mpeg");
        };
    }
}
