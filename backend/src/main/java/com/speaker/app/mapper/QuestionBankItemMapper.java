package com.speaker.app.mapper;

import com.speaker.app.model.entity.QuestionBankItem;
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

    List<QuestionBankItem> findBySeasonPartTopicOrderBySortOrderAsc(
            @Param("seasonLabel") String seasonLabel,
            @Param("part") String part,
            @Param("topic") String topic);

    List<String> findDistinctTopicsBySeasonAndPart(
            @Param("seasonLabel") String seasonLabel,
            @Param("part") String part);

    /** 同一 topic 下先 PART2（按 sort_order）再 PART3 */
    List<QuestionBankItem> findBySeasonLabelAndTopicOrderPart2ThenPart3(
            @Param("seasonLabel") String seasonLabel,
            @Param("topic") String topic);

    /** 多个 part 下去重 topic，按 topic 字母序 */
    List<String> findDistinctTopicsBySeasonAndParts(
            @Param("seasonLabel") String seasonLabel,
            @Param("parts") List<String> parts);

    List<String> findDistinctSeasonLabels();

    List<QuestionBankItem> findByTopicContainingIgnoreCase(@Param("q") String q);

    List<QuestionBankItem> findBySeasonLabelAndPartsNotDeleted(
            @Param("seasonLabel") String seasonLabel,
            @Param("parts") List<String> parts);

    int updateById(QuestionBankItem item);

    int softDeleteById(@Param("id") Long id);

    /** 将某季节下所有未删题目标记为软删除 */
    int softDeleteBySeasonLabel(@Param("seasonLabel") String seasonLabel);
}
