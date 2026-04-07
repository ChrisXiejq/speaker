package com.speaker.app.web;

import com.speaker.app.dto.BankTopicGroupResponse;
import com.speaker.app.mapper.QuestionBankItemMapper;
import com.speaker.app.model.entity.QuestionBankItem;
import com.speaker.app.speaking.SeasonLabelResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bank")
public class BankController {

    private static final String PART1 = "PART1";
    private static final String PART2_AND_3 = "PART2_AND_3";

    private final QuestionBankItemMapper bankMapper;
    private final SeasonLabelResolver seasonLabelResolver;

    public BankController(QuestionBankItemMapper bankMapper, SeasonLabelResolver seasonLabelResolver) {
        this.bankMapper = bankMapper;
        this.seasonLabelResolver = seasonLabelResolver;
    }

    /**
     * 返回库中所有未删除的季节标签；列表首项为根据当前日期解析出的「默认当季」。
     * 若无任何题目则返回空列表（不再使用配置文件中的默认季节）。
     */
    @GetMapping("/seasons")
    public List<String> seasons() {
        List<String> labels = bankMapper.findDistinctSeasonLabels();
        if (labels.isEmpty()) {
            return List.of();
        }
        String def = seasonLabelResolver.pickForNow(labels).orElse(labels.get(0));
        return seasonLabelResolver.orderWithDefaultFirst(labels, def);
    }

    /**
     * 按 topic 分组返回。
     * <ul>
     *   <li>{@code part=PART1}：组内为 Part 1 题目，字段 {@code questions}</li>
     *   <li>{@code part=PART2_AND_3}：同一 topic 下 {@code part2Questions} 在前、{@code part3Questions} 在后</li>
     * </ul>
     */
    @GetMapping("/questions")
    public List<BankTopicGroupResponse> questions(
            @RequestParam(required = false) String season,
            @RequestParam String part) {
        List<String> labels = bankMapper.findDistinctSeasonLabels();
        String s = season != null && !season.isBlank()
                ? season.trim()
                : seasonLabelResolver.pickForNow(labels).orElse(null);
        if (s == null) {
            return List.of();
        }
        String p = part != null ? part.trim() : "";
        if (PART1.equalsIgnoreCase(p)) {
            List<QuestionBankItem> flat = bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(s, PART1);
            return BankTopicGroupResponse.fromFlat(flat);
        }
        if (PART2_AND_3.equalsIgnoreCase(p) || "PART23".equalsIgnoreCase(p)) {
            List<QuestionBankItem> p2 = bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(s, "PART2");
            List<QuestionBankItem> p3 = bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(s, "PART3");
            return BankTopicGroupResponse.fromPart2AndPart3(p2, p3);
        }
        throw new IllegalArgumentException("part 须为 PART1 或 PART2_AND_3");
    }

    @GetMapping("/search")
    public List<QuestionBankItem> search(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return bankMapper.findByTopicContainingIgnoreCase(q.trim());
    }
}
