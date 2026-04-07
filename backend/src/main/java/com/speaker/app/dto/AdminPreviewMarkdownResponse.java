package com.speaker.app.dto;

import com.speaker.app.model.entity.QuestionBankItem;

import java.util.List;

public record AdminPreviewMarkdownResponse(
        int part1Count,
        int part2Count,
        int part3Count,
        List<String> warnings,
        List<QuestionBankItem> sampleItems
) {
}
