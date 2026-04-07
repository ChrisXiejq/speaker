package com.speaker.app.service;

import com.speaker.app.dto.AdminBankItemUpdateRequest;
import com.speaker.app.mapper.QuestionBankItemMapper;
import com.speaker.app.model.entity.QuestionBankItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminBankCrudService {

    private final QuestionBankItemMapper bankMapper;

    public AdminBankCrudService(QuestionBankItemMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    public List<QuestionBankItem> listItems(String seasonLabel, String segment) {
        if (seasonLabel == null || seasonLabel.isBlank()) {
            throw new IllegalArgumentException("seasonLabel 不能为空");
        }
        String seg = segment.trim().toLowerCase();
        return switch (seg) {
            case "part1" -> bankMapper.findBySeasonLabelAndPartsNotDeleted(seasonLabel.trim(), List.of("PART1"));
            case "part23" -> bankMapper.findBySeasonLabelAndPartsNotDeleted(seasonLabel.trim(), List.of("PART2", "PART3"));
            default -> throw new IllegalArgumentException("segment 须为 part1 或 part23");
        };
    }

    @Transactional
    public void updateItem(long id, AdminBankItemUpdateRequest req) {
        QuestionBankItem existing = bankMapper.findById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
            throw new IllegalArgumentException("题目不存在或已删除");
        }
        Integer sortOrder = req.sortOrder() != null ? req.sortOrder() : existing.getSortOrder();
        String kw = req.keywordsJson();
        if (kw != null) {
            kw = kw.trim();
            if (kw.isEmpty()) {
                kw = null;
            }
        }
        String ans = req.answerText();
        if (ans != null) {
            ans = ans.trim();
            if (ans.isEmpty()) {
                ans = null;
            }
        }
        QuestionBankItem row = QuestionBankItem.builder()
                .id(id)
                .topic(req.topic().trim())
                .questionText(req.questionText().trim())
                .answerText(ans)
                .keywordsJson(kw)
                .sortOrder(sortOrder)
                .build();
        int n = bankMapper.updateById(row);
        if (n == 0) {
            throw new IllegalArgumentException("更新失败");
        }
    }

    @Transactional
    public void softDelete(long id) {
        int n = bankMapper.softDeleteById(id);
        if (n == 0) {
            throw new IllegalArgumentException("题目不存在或已删除");
        }
    }

    /** 软删除某季节下全部题目（未删行） */
    @Transactional
    public int softDeleteBySeasonLabel(String seasonLabel) {
        if (seasonLabel == null || seasonLabel.isBlank()) {
            throw new IllegalArgumentException("seasonLabel 不能为空");
        }
        return bankMapper.softDeleteBySeasonLabel(seasonLabel.trim());
    }
}
