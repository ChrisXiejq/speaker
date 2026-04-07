package com.speakingkiller.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PracticeStartRequest(
        @NotBlank String part,
        @NotBlank String topicSource,
        String customTopic,
        Long bankQuestionId,
        String season
) {}
