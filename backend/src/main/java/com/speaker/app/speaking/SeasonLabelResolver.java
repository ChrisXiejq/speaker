package com.speaker.app.speaking;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 根据 {@code season_label} 字符串解析考季时间范围，并选出相对「当前日期」最应展示的一季。
 * <p>
 * 支持格式：
 * <ul>
 *   <li>{@code 2025Q1}～{@code 2025Q4}：自然季度</li>
 *   <li>{@code 2025-9-12}：雅思常见写法，表示同年第 9～12 月（9 月至 12 月）</li>
 * </ul>
 * 无法解析的标签仍参与列表展示，选取默认季时按字符串降序作为兜底（与旧版 ORDER BY DESC 接近）。
 */
@Component
public class SeasonLabelResolver {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

    private static final Pattern P_QUARTER = Pattern.compile("^(\\d{4})[Qq]([1-4])$");
    /** 同年月份区间，如 2025-9-12 → 2025-09-01 ～ 2025-12-31 */
    private static final Pattern P_MONTH_RANGE = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");

    public record DateRange(LocalDate start, LocalDate end) {
    }

    public LocalDate today() {
        return LocalDate.now(CN);
    }

    public Optional<DateRange> parse(String label) {
        if (label == null) {
            return Optional.empty();
        }
        String t = label.trim();
        if (t.isEmpty()) {
            return Optional.empty();
        }

        Matcher mq = P_QUARTER.matcher(t);
        if (mq.matches()) {
            int year = Integer.parseInt(mq.group(1));
            int q = Integer.parseInt(mq.group(2));
            int startMonth = (q - 1) * 3 + 1;
            LocalDate start = LocalDate.of(year, startMonth, 1);
            LocalDate end = start.plusMonths(3).minusDays(1);
            return Optional.of(new DateRange(start, end));
        }

        Matcher mm = P_MONTH_RANGE.matcher(t);
        if (mm.matches()) {
            int y = Integer.parseInt(mm.group(1));
            int a = Integer.parseInt(mm.group(2));
            int b = Integer.parseInt(mm.group(3));
            if (a >= 1 && a <= 12 && b >= 1 && b <= 12 && a <= b) {
                LocalDate start = LocalDate.of(y, a, 1);
                LocalDate end = LocalDate.of(y, b, 1).with(TemporalAdjusters.lastDayOfMonth());
                return Optional.of(new DateRange(start, end));
            }
        }

        return Optional.empty();
    }

    /**
     * 在若干季节标签中选出应对「今天」默认展示的一季；无标签时为空。
     */
    public Optional<String> pickForNow(List<String> labels) {
        return pickForDate(labels, today());
    }

    public Optional<String> pickForDate(List<String> labels, LocalDate day) {
        if (labels == null || labels.isEmpty()) {
            return Optional.empty();
        }
        List<LabelRange> parsed = new ArrayList<>();
        List<String> unparsed = new ArrayList<>();
        for (String raw : labels) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String label = raw.trim();
            Optional<DateRange> r = parse(label);
            if (r.isPresent()) {
                parsed.add(new LabelRange(label, r.get()));
            } else {
                unparsed.add(label);
            }
        }

        if (!parsed.isEmpty()) {
            Optional<String> active = parsed.stream()
                    .filter(lr -> !day.isBefore(lr.range().start()) && !day.isAfter(lr.range().end()))
                    .max(Comparator.comparing(lr -> lr.range().start()))
                    .map(LabelRange::label);
            if (active.isPresent()) {
                return active;
            }
            Optional<String> past = parsed.stream()
                    .filter(lr -> lr.range().end().isBefore(day))
                    .max(Comparator.comparing(lr -> lr.range().end()))
                    .map(LabelRange::label);
            if (past.isPresent()) {
                return past;
            }
            Optional<String> future = parsed.stream()
                    .filter(lr -> lr.range().start().isAfter(day))
                    .min(Comparator.comparing(lr -> lr.range().start()))
                    .map(LabelRange::label);
            if (future.isPresent()) {
                return future;
            }
            return parsed.stream()
                    .max(Comparator.comparing(lr -> lr.range().end()))
                    .map(LabelRange::label);
        }

        return unparsed.stream().max(String::compareTo);
    }

    /**
     * 将默认季放在第一位，其余按「结束时间」新到旧排序；无法解析的排在解析项之后，字符串降序。
     */
    public List<String> orderWithDefaultFirst(List<String> labels, String defaultLabel) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String s : labels) {
            if (s != null && !s.isBlank()) {
                uniq.add(s.trim());
            }
        }
        List<String> out = new ArrayList<>();
        if (defaultLabel != null && !defaultLabel.isBlank() && uniq.remove(defaultLabel.trim())) {
            out.add(defaultLabel.trim());
        }
        List<String> rest = new ArrayList<>(uniq);
        rest.sort(this::compareByRecencyDesc);
        out.addAll(rest);
        return out;
    }

    private int compareByRecencyDesc(String a, String b) {
        Optional<DateRange> ra = parse(a);
        Optional<DateRange> rb = parse(b);
        if (ra.isPresent() && rb.isPresent()) {
            int c = rb.get().end().compareTo(ra.get().end());
            if (c != 0) {
                return c;
            }
            return rb.get().start().compareTo(ra.get().start());
        }
        if (ra.isPresent()) {
            return -1;
        }
        if (rb.isPresent()) {
            return 1;
        }
        return b.compareTo(a);
    }

    private record LabelRange(String label, DateRange range) {
    }
}
