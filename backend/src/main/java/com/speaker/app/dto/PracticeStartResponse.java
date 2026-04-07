package com.speaker.app.dto;

public record PracticeStartResponse(
        long sessionId,
        String examinerLine,
        String part,
        String topic,
        /** 本题库小题的参考答案，非题库模式为 null */
        String referenceAnswer,
        /** 关键词 JSON 字符串，非题库模式为 null */
        String keywordsJson,
        /** 是否启用「按话题顺序题库」流程（题库模式为 true） */
        boolean bankTopicFlow,
        /** BANK / AI_EXPAND / AWAIT_NEXT_TOPIC / LEGACY */
        String practicePhase
) {}
