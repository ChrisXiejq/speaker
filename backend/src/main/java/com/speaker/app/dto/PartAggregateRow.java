package com.speaker.app.dto;

import lombok.Data;

/** MyBatis：按 Part 聚合已完成会话 + 报告 */
@Data
public class PartAggregateRow {
    private String part;
    private Long sessionCount;
    private Double avgOverallBand;
    private Double avgPronunciation;
    private Double avgGrammar;
    private Double avgCoherence;
    private Double avgFluency;
    private Double avgIdeas;
}
