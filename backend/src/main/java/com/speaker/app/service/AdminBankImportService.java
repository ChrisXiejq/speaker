package com.speaker.app.service;

import com.speaker.app.dto.AdminImportMarkdownRequest;
import com.speaker.app.dto.AdminImportMarkdownResponse;
import com.speaker.app.dto.AdminPreviewMarkdownResponse;
import com.speaker.app.mapper.QuestionBankItemMapper;
import com.speaker.app.model.entity.QuestionBankItem;
import com.speaker.app.service.markdown.BankMarkdownParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminBankImportService {

    private final QuestionBankItemMapper bankMapper;

    public AdminBankImportService(QuestionBankItemMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    public AdminPreviewMarkdownResponse preview(AdminImportMarkdownRequest req) {
        validateMarkdown(req);
        List<QuestionBankItem> all = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        BankMarkdownParser.ParseResult p1 = BankMarkdownParser.parsePart1(req.part1Markdown());
        BankMarkdownParser.ParseResult p23 = BankMarkdownParser.parsePart2And3(req.part23Markdown());
        all.addAll(p1.items());
        all.addAll(p23.items());
        warnings.addAll(p1.warnings());
        warnings.addAll(p23.warnings());
        List<QuestionBankItem> sample = all.stream().limit(15).collect(Collectors.toList());
        return new AdminPreviewMarkdownResponse(
                countPart(all, "PART1"),
                countPart(all, "PART2"),
                countPart(all, "PART3"),
                warnings,
                sample);
    }

    @Transactional
    public AdminImportMarkdownResponse importMarkdown(AdminImportMarkdownRequest req) {
        validateMarkdown(req);
        BankMarkdownParser.ParseResult p1 = BankMarkdownParser.parsePart1(req.part1Markdown());
        BankMarkdownParser.ParseResult p23 = BankMarkdownParser.parsePart2And3(req.part23Markdown());
        List<QuestionBankItem> items = new ArrayList<>();
        items.addAll(p1.items());
        items.addAll(p23.items());
        List<String> warnings = new ArrayList<>();
        warnings.addAll(p1.warnings());
        warnings.addAll(p23.warnings());
        if (items.isEmpty()) {
            return new AdminImportMarkdownResponse(0, 0, 0, 0, warnings);
        }
        String season = req.seasonLabel().trim();
        if (req.replaceExisting()) {
            bankMapper.softDeleteBySeasonLabel(season);
        }
        Map<String, Integer> sortByPart = new HashMap<>();
        int inserted = 0;
        for (QuestionBankItem raw : items) {
            int next = sortByPart.merge(raw.getPart(), 1, Integer::sum);
            QuestionBankItem row = QuestionBankItem.builder()
                    .seasonLabel(season)
                    .part(raw.getPart())
                    .topic(raw.getTopic())
                    .questionText(raw.getQuestionText())
                    .answerText(raw.getAnswerText())
                    .keywordsJson(raw.getKeywordsJson())
                    .sortOrder(next)
                    .build();
            bankMapper.insert(row);
            inserted++;
        }
        return new AdminImportMarkdownResponse(
                inserted,
                countPart(items, "PART1"),
                countPart(items, "PART2"),
                countPart(items, "PART3"),
                warnings);
    }

    private static void validateMarkdown(AdminImportMarkdownRequest req) {
        boolean empty1 = req.part1Markdown() == null || req.part1Markdown().isBlank();
        boolean empty23 = req.part23Markdown() == null || req.part23Markdown().isBlank();
        if (empty1 && empty23) {
            throw new IllegalArgumentException("请至少填写 Part1 或 Part2&3 其中一段 Markdown");
        }
    }

    private static int countPart(List<QuestionBankItem> items, String part) {
        return (int) items.stream().filter(i -> part.equals(i.getPart())).count();
    }
}
