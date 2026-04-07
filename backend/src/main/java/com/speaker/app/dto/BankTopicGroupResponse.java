package com.speaker.app.dto;

import com.speaker.app.model.entity.QuestionBankItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按 topic 聚合；Part 1 使用 {@link #questions}；
 * Part 2 &amp; 3 合并展示时使用 {@link #part2Questions} 与 {@link #part3Questions}（同一 topic 下先 Part2 后 Part3）。
 */
public record BankTopicGroupResponse(
        String topic,
        /** Part 1：本题话题下全部小题 */
        List<QuestionBankItem> questions,
        /** Part 2 &amp; 3：本题话题下 Part 2 题目（可为空列表） */
        List<QuestionBankItem> part2Questions,
        /** Part 2 &amp; 3：本题话题下 Part 3 题目（可为空列表） */
        List<QuestionBankItem> part3Questions
) {

    public static BankTopicGroupResponse part1(String topic, List<QuestionBankItem> qs) {
        return new BankTopicGroupResponse(topic, List.copyOf(qs), List.of(), List.of());
    }

    public static BankTopicGroupResponse part23(String topic, List<QuestionBankItem> p2, List<QuestionBankItem> p3) {
        return new BankTopicGroupResponse(topic, List.of(), List.copyOf(p2), List.copyOf(p3));
    }

    public static List<BankTopicGroupResponse> fromFlat(List<QuestionBankItem> items) {
        Map<String, List<QuestionBankItem>> map = new LinkedHashMap<>();
        for (QuestionBankItem item : items) {
            String t = topicKey(item.getTopic());
            map.computeIfAbsent(t, k -> new ArrayList<>()).add(item);
        }
        List<BankTopicGroupResponse> out = new ArrayList<>();
        for (Map.Entry<String, List<QuestionBankItem>> e : map.entrySet()) {
            out.add(part1(e.getKey(), e.getValue()));
        }
        return out;
    }

    /**
     * Part2、Part3 两路列表按 topic 合并；每个 topic 内保持 Part2 在前、Part3 在后。
     */
    public static List<BankTopicGroupResponse> fromPart2AndPart3(
            List<QuestionBankItem> part2Items,
            List<QuestionBankItem> part3Items) {
        Map<String, List<QuestionBankItem>> m2 = groupByTopic(part2Items);
        Map<String, List<QuestionBankItem>> m3 = groupByTopic(part3Items);
        Set<String> order = new LinkedHashSet<>();
        m2.keySet().forEach(order::add);
        m3.keySet().forEach(order::add);
        List<BankTopicGroupResponse> out = new ArrayList<>();
        for (String t : order) {
            out.add(part23(t, m2.getOrDefault(t, List.of()), m3.getOrDefault(t, List.of())));
        }
        return out;
    }

    private static Map<String, List<QuestionBankItem>> groupByTopic(List<QuestionBankItem> items) {
        Map<String, List<QuestionBankItem>> map = new LinkedHashMap<>();
        for (QuestionBankItem item : items) {
            String t = topicKey(item.getTopic());
            map.computeIfAbsent(t, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private static String topicKey(String topic) {
        String t = topic != null ? topic.trim() : "";
        return t.isEmpty() ? "（未命名话题）" : t;
    }
}
