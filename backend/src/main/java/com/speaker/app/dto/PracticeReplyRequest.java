package com.speaker.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PracticeReplyRequest(@NotBlank String userText) {}
