package com.speaker.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PracticeStartRequest(
        @NotBlank String part,
        @NotBlank String topicSource,
        String customTopic,
        Long bankQuestionId,
        String season,
        /** 仅题库模式：是否允许在问完本题话题库题后由 AI 继续扩展提问 */
        Boolean allowAiExpand
) {}
