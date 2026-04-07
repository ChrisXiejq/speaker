package com.speaker.app.dto;

import java.util.List;

public record AdminImportMarkdownResponse(
        int inserted,
        int part1Count,
        int part2Count,
        int part3Count,
        List<String> warnings
) {
}
