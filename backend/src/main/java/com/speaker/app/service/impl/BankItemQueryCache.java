package com.speaker.app.service.impl;

import com.speaker.app.cache.BankCacheNames;
import com.speaker.app.model.entity.QuestionBankItem;
import com.speaker.app.repository.QuestionBankItemMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按季节+Part 缓存题目行。与 {@link BankCatalogServiceImpl} 分离，确保 {@code @Cacheable} 走 Spring 代理。
 */
@Service
public class BankItemQueryCache {

    private final QuestionBankItemMapper bankMapper;

    public BankItemQueryCache(QuestionBankItemMapper bankMapper) {
        this.bankMapper = bankMapper;
    }

    /** key 含 v2：避免与曾缓存 record DTO 的旧条目混用 */
    @Cacheable(cacheNames = BankCacheNames.BANK_TOPIC_GROUPS, key = "'v2:' + #season + ':' + #part")
    public List<QuestionBankItem> findBySeasonAndPart(String season, String part) {
        return bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(season, part);
    }
}
