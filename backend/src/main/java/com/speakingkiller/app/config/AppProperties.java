package com.speakingkiller.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Security security = new Security();
    private Dashscope dashscope = new Dashscope();
    private Wechat wechat = new Wechat();
    private Speaking speaking = new Speaking();

    @Getter
    @Setter
    public static class Speaking {
        /** 当季题库标签，可与前端「当季」展示一致 */
        private String currentSeason = "2025Q1";
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
    }

    @Getter
    @Setter
    public static class Security {
        private RateLimit rateLimit = new RateLimit();

        @Getter
        @Setter
        public static class RateLimit {
            private int authPerMinute = 20;
            private int apiPerMinute = 120;
            private int aiPerMinute = 30;
        }
    }

    @Getter
    @Setter
    public static class Dashscope {
        private String apiKey;
        private String chatModel = "qwen-turbo";
        /** OpenAI 兼容 ASR，见百炼 Qwen-ASR 文档 */
        private String asrModel = "qwen3-asr-flash";
        /** CosyVoice 非流式合成 */
        private String ttsModel = "cosyvoice-v3-flash";
        /** 系统音色，可按控制台音色列表调整 */
        private String ttsVoice = "longanyang";
        private String ttsFormat = "mp3";
        private int ttsSampleRate = 24000;
        /** 单次 TTS 最大字符数，防止滥用 */
        private int ttsMaxChars = 8000;
    }

    @Getter
    @Setter
    public static class Wechat {
        private String miniAppId;
        private String miniAppSecret;
    }
}
