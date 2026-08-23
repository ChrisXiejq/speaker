package com.speaker.app.service.impl;

import com.speaker.app.dto.PartAggregateRow;
import com.speaker.app.dto.PracticeDashboardDto;
import com.speaker.app.dto.SessionTrendRow;
import com.speaker.app.model.entity.User;
import com.speaker.app.repository.PracticeSessionMapper;
import com.speaker.app.service.intf.CurrentUserServiceIntf;
import com.speaker.app.service.intf.PracticeDashboardServiceIntf;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PracticeDashboardServiceImpl implements PracticeDashboardServiceIntf {

    private final PracticeSessionMapper sessionMapper;
    private final CurrentUserServiceIntf currentUserService;

    public PracticeDashboardServiceImpl(
            PracticeSessionMapper sessionMapper, CurrentUserServiceIntf currentUserService) {
        this.sessionMapper = sessionMapper;
        this.currentUserService = currentUserService;
    }

    @Override
    public PracticeDashboardDto getDashboardStats() {
        User user = currentUserService.requireCurrentUser();
        long uid = user.getId();
        long completed = sessionMapper.countCompletedWithReport(uid);
        long topics = sessionMapper.countDistinctTopicsCompleted(uid);
        Double overall = sessionMapper.avgOverallBandAll(uid);
        List<PartAggregateRow> rows = sessionMapper.aggregateCompletedByPart(uid);
        rows.sort(Comparator.comparingInt(a -> partOrder(a.getPart())));
        List<PracticeDashboardDto.PartStatItem> byPart = rows.stream()
                .map(r -> new PracticeDashboardDto.PartStatItem(
                        r.getPart(),
                        partLabel(r.getPart()),
                        r.getSessionCount() == null ? 0L : r.getSessionCount(),
                        r.getAvgOverallBand(),
                        r.getAvgPronunciation(),
                        r.getAvgGrammar(),
                        r.getAvgCoherence(),
                        r.getAvgFluency(),
                        r.getAvgIdeas()))
                .toList();
        List<SessionTrendRow> trendRows = sessionMapper.recentCompletedTrend(uid, 48);
        trendRows.sort(Comparator.comparingLong(SessionTrendRow::getStartedAt));
        List<PracticeDashboardDto.ScoreTrendPoint> recentScores = new ArrayList<>();
        for (SessionTrendRow tr : trendRows) {
            Double b = parseOverallBand(tr.getOverallBand());
            if (b == null || tr.getStartedAt() == null) {
                continue;
            }
            recentScores.add(new PracticeDashboardDto.ScoreTrendPoint(
                    tr.getStartedAt(), b, partLabel(tr.getPart())));
        }
        return new PracticeDashboardDto(completed, topics, overall, byPart, recentScores);
    }

    private static int partOrder(String part) {
        if (part == null) {
            return 99;
        }
        return switch (part) {
            case "PART1" -> 1;
            case "PART2" -> 2;
            case "PART3" -> 3;
            case "PART2_AND_3" -> 4;
            default -> 99;
        };
    }

    private static String partLabel(String part) {
        if (part == null) {
            return "";
        }
        return switch (part) {
            case "PART1" -> "Part 1";
            case "PART2" -> "Part 2";
            case "PART3" -> "Part 3";
            case "PART2_AND_3" -> "Part 2 & 3";
            default -> part;
        };
    }

    private static Double parseOverallBand(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
