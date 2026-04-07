package com.speakingkiller.app.web;

import com.speakingkiller.app.config.AppProperties;
import com.speakingkiller.app.mapper.QuestionBankItemMapper;
import com.speakingkiller.app.model.entity.QuestionBankItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final QuestionBankItemMapper bankMapper;
    private final AppProperties appProperties;

    public BankController(QuestionBankItemMapper bankMapper, AppProperties appProperties) {
        this.bankMapper = bankMapper;
        this.appProperties = appProperties;
    }

    @GetMapping("/seasons")
    public List<String> seasons() {
        List<String> fromDb = bankMapper.findDistinctSeasonLabels();
        if (fromDb.isEmpty()) {
            return List.of(appProperties.getSpeaking().getCurrentSeason());
        }
        return fromDb;
    }

    @GetMapping("/questions")
    public List<QuestionBankItem> questions(
            @RequestParam(required = false) String season,
            @RequestParam String part) {
        String s = season != null && !season.isBlank()
                ? season
                : appProperties.getSpeaking().getCurrentSeason();
        return bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(s, part);
    }

    @GetMapping("/search")
    public List<QuestionBankItem> search(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return bankMapper.findByTopicContainingIgnoreCase(q.trim());
    }
}
