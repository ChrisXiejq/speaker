package com.speaker.app.dto;

import java.util.List;

/**
 * 首页练习看板：已完成会话、话题覆盖、按 Part 汇总与近期分数走势。
 */
public record PracticeDashboardDto(
        long completedSessionCount,
        long distinctTopicCount,
        Double overallAvgBand,
        List<PartStatItem> byPart,
        List<ScoreTrendPoint> recentScores
) {
    public record PartStatItem(
            String partKey,
            String partLabel,
            long sessionCount,
            Double avgOverallBand,
            Double avgPronunciation,
            Double avgGrammar,
            Double avgCoherence,
            Double avgFluency,
            Double avgIdeas
    ) {}

    /** 时间正序（旧 → 新），便于折线图 */
    public record ScoreTrendPoint(long startedAtMillis, Double overallBand, String partLabel) {}
}
