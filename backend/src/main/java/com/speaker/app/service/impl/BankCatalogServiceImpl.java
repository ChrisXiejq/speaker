package com.speaker.app.service.impl;

import com.speaker.app.cache.BankCacheNames;
import com.speaker.app.dto.BankTopicGroupResponse;
import com.speaker.app.model.entity.QuestionBankItem;
import com.speaker.app.repository.QuestionBankItemMapper;
import com.speaker.app.common.utils.SeasonLabelResolver;
import com.speaker.app.service.intf.BankCatalogServiceIntf;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankCatalogServiceImpl implements BankCatalogServiceIntf {

    private static final String PART1 = "PART1";
    private static final String PART2_AND_3 = "PART2_AND_3";

    private final QuestionBankItemMapper bankMapper;
    private final SeasonLabelResolver seasonLabelResolver;
    private final BankItemQueryCache itemQueryCache;

    public BankCatalogServiceImpl(
            QuestionBankItemMapper bankMapper,
            SeasonLabelResolver seasonLabelResolver,
            BankItemQueryCache itemQueryCache) {
        this.bankMapper = bankMapper;
        this.seasonLabelResolver = seasonLabelResolver;
        this.itemQueryCache = itemQueryCache;
    }

    @Override
    @Cacheable(cacheNames = BankCacheNames.BANK_SEASONS, key = "'v1'")
    public List<String> listSeasonsForApi() {
        List<String> labels = bankMapper.findDistinctSeasonLabels();
        if (labels.isEmpty()) {
            return List.of();
        }
        String def = seasonLabelResolver.pickForNow(labels).orElse(labels.get(0));
        return seasonLabelResolver.orderWithDefaultFirst(labels, def);
    }

    @Override
    public List<String> getDistinctSeasonLabels() {
        return bankMapper.findDistinctSeasonLabels();
    }

    @Override
    public List<BankTopicGroupResponse> listTopicGroups(String season, String partKey) {
        if (PART1.equals(partKey)) {
            List<QuestionBankItem> flat = itemQueryCache.findBySeasonAndPart(season, PART1);
            return BankTopicGroupResponse.fromFlat(flat);
        }
        if (PART2_AND_3.equals(partKey)) {
            List<QuestionBankItem> p2 = itemQueryCache.findBySeasonAndPart(season, "PART2");
            List<QuestionBankItem> p3 = itemQueryCache.findBySeasonAndPart(season, "PART3");
            return BankTopicGroupResponse.fromPart2AndPart3(p2, p3);
        }
        throw new IllegalArgumentException("partKey 须为 PART1 或 PART2_AND_3");
    }
}
