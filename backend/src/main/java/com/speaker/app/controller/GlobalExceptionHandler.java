package com.speaker.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        log.warn("[handler] -> 400 IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("通义")
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.BAD_REQUEST;
        log.warn("[handler] -> {} IllegalStateException: {}", status.value(), ex.getMessage());
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        HttpStatusCode code = ex.getStatusCode();
        String msg = ex.getReason() != null ? ex.getReason() : "请求失败";
        log.warn("[handler] -> {} ResponseStatusException: {}", code.value(), msg);
        return ResponseEntity.status(code).body(Map.of("error", msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("参数不合法");
        log.warn("[handler] -> 400 MethodArgumentNotValidException: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex) {
        log.warn("[handler] -> 409 DataIntegrityViolationException", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "数据冲突，请检查唯一约束"));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> io(IOException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "文件读取失败";
        log.warn("[handler] -> 400 IOException: {}", msg, ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }

    /**
     * 题库等接口启用 Redis 缓存但连不上时（本机指向公网 IP、Redis 只监听 127.0.0.1、安全组未放行 6379 等）。
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<Map<String, Object>> redisDown(RedisConnectionFailureException ex) {
        log.error("[handler] -> 503 RedisConnectionFailureException", ex);
        String hint =
                "无法连接 Redis。若在个人电脑运行后端而 Redis 只在服务器上，请将 app.cache.use-redis 设为 false，"
                        + "或在服务器上将 Redis 与后端同机并使用 127.0.0.1，并确保 Redis 已启动且端口可访问。"
                        + " 详情: "
                        + (ex.getMessage() != null ? ex.getMessage() : "unknown");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", hint));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> other(Exception ex) {
        log.error("[handler] -> 500 未单独处理的异常（响应仍为通用文案）", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "服务器内部错误"));
    }
}
