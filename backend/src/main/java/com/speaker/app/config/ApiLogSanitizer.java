package com.speaker.app.config;

import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * 接口日志用：截断过长内容，避免打印 token/密码等（仅做简单关键字脱敏）。
 */
public final class ApiLogSanitizer {

    private static final int MAX_LEN = 800;

    private ApiLogSanitizer() {}

    public static String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("arg").append(i).append('=').append(summarizeOne(args[i]));
        }
        return truncate(sb.toString());
    }

    private static String summarizeOne(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof MultipartFile f) {
            return "MultipartFile(name=" + f.getOriginalFilename() + ", size=" + f.getSize() + ")";
        }
        if (o instanceof jakarta.servlet.ServletRequest || o instanceof jakarta.servlet.ServletResponse) {
            return o.getClass().getSimpleName();
        }
        if (o instanceof byte[] b) {
            return "byte[" + b.length + "]";
        }
        if (o.getClass().isArray()) {
            int n = Array.getLength(o);
            return o.getClass().getComponentType().getSimpleName() + "[" + n + "]";
        }
        if (o instanceof Collection<?> c) {
            return o.getClass().getSimpleName() + "(size=" + c.size() + ")";
        }
        if (o instanceof Map<?, ?> m) {
            return o.getClass().getSimpleName() + "(size=" + m.size() + ")";
        }
        String s = String.valueOf(o);
        s = maskSensitive(s);
        return truncate(s);
    }

    public static String summarizeResult(Object result) {
        if (result == null) {
            return "null";
        }
        if (result instanceof org.springframework.http.ResponseEntity<?> re) {
            Object body = re.getBody();
            String bodyStr = body == null ? "null" : summarizeOne(body);
            return "ResponseEntity(status=" + re.getStatusCode().value() + ", body=" + bodyStr + ")";
        }
        if (result instanceof Collection<?> c) {
            return result.getClass().getSimpleName() + "(size=" + c.size() + ")";
        }
        if (result instanceof Map<?, ?> m) {
            return result.getClass().getSimpleName() + "(size=" + m.size() + ")";
        }
        if (result instanceof CharSequence || result instanceof Number || result instanceof Boolean) {
            return truncate(maskSensitive(String.valueOf(result)));
        }
        return result.getClass().getSimpleName() + "@" + Integer.toHexString(result.hashCode());
    }

    private static String maskSensitive(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        String lower = s.toLowerCase();
        if (lower.contains("password")
                || lower.contains("secret")
                || lower.contains("token")
                || lower.contains("authorization")
                || lower.contains("api-key")
                || lower.contains("apikey")) {
            return "[redacted:可能含敏感字段]";
        }
        return s;
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_LEN) {
            return s;
        }
        return s.substring(0, MAX_LEN) + "...(truncated)";
    }
}
