package com.speaker.app.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 对所有 {@code @RestController} 的接口方法记录入参摘要、耗时、返回摘要；异常时打日志后抛出，由 {@code GlobalExceptionHandler} 统一转换响应（完整堆栈在 Handler 中记录）。
 */
@Aspect
@Component
@Order
public class ControllerLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerLoggingAspect.class);

    @Around("@within(org.springframework.web.bind.annotation.RestController) && within(com.speaker.app.controller..*)")
    public Object logApi(ProceedingJoinPoint pjp) throws Throwable {
        String cls = pjp.getTarget().getClass().getSimpleName();
        String method = pjp.getSignature().getName();
        String sig = cls + "." + method;
        String args = ApiLogSanitizer.summarizeArgs(pjp.getArgs());
        String rid = MDC.get(RequestLoggingFilter.MDC_REQUEST_ID);
        long t0 = System.nanoTime();
        log.info("[api] begin requestId={} {} args=[{}]", rid, sig, args);
        try {
            Object result = pjp.proceed();
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            log.info(
                    "[api] ok requestId={} {} {}ms result={}",
                    rid,
                    sig,
                    ms,
                    ApiLogSanitizer.summarizeResult(result));
            return result;
        } catch (Throwable ex) {
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            log.warn(
                    "[api] fail requestId={} {} {}ms {} : {}",
                    rid,
                    sig,
                    ms,
                    ex.getClass().getName(),
                    ex.getMessage());
            throw ex;
        }
    }
}
