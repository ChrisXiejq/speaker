package com.speaker.app.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminBankItemUpdateRequest(
        @NotBlank String topic,
        @NotBlank String questionText,
        String answerText,
        String keywordsJson,
        Integer sortOrder
) {
}
