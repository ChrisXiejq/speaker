package com.speakingkiller.app.mapper;

import com.speakingkiller.app.model.entity.QuestionBankItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionBankItemMapper {

    int insert(QuestionBankItem item);

    QuestionBankItem findById(@Param("id") Long id);

    long count();

    List<QuestionBankItem> findBySeasonLabelAndPartOrderBySortOrderAsc(
            @Param("seasonLabel") String seasonLabel,
            @Param("part") String part);

    List<String> findDistinctSeasonLabels();

    List<QuestionBankItem> findByTopicContainingIgnoreCase(@Param("q") String q);
}
