package com.speakingkiller.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PracticeReplyRequest(@NotBlank String userText) {}
