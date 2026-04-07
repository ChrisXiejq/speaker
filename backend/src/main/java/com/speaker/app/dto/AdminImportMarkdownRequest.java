package com.speaker.app.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminImportMarkdownRequest(
        @NotBlank String seasonLabel,
        String part1Markdown,
        String part23Markdown,
        boolean replaceExisting
) {
}
