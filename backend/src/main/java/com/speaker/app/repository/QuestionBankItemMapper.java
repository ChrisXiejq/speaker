package com.speaker.app.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speaker.app.model.entity.QuestionBankItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface QuestionBankItemMapper extends BaseMapper<QuestionBankItem> {

    default QuestionBankItem findById(Long id) {
        return selectById(id);
    }

    @Select("SELECT COUNT(*) FROM question_bank_items")
    long count();

    @Select(
            "SELECT * FROM question_bank_items WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND part = #{part} AND is_deleted = 0 ORDER BY sort_order ASC")
    List<QuestionBankItem> findBySeasonLabelAndPartOrderBySortOrderAsc(
            @Param("seasonLabel") String seasonLabel, @Param("part") String part);

    /** 随机抽一题，避免拉全量 Part 再在内存里 random（开始模拟时显著减负） */
    @Select(
            "SELECT * FROM question_bank_items WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND part = #{part} AND is_deleted = 0 ORDER BY RAND() LIMIT 1")
    QuestionBankItem findRandomOneBySeasonAndPart(
            @Param("seasonLabel") String seasonLabel, @Param("part") String part);

    @Select(
            "SELECT * FROM question_bank_items WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND is_deleted = 0 AND part IN ('PART2','PART3') ORDER BY RAND() LIMIT 1")
    QuestionBankItem findRandomOneBySeasonPart2Or3(@Param("seasonLabel") String seasonLabel);

    @Select(
            "SELECT * FROM question_bank_items WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND part = #{part} AND topic = #{topic} AND is_deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<QuestionBankItem> findBySeasonPartTopicOrderBySortOrderAsc(
            @Param("seasonLabel") String seasonLabel,
            @Param("part") String part,
            @Param("topic") String topic);

    @Select(
            "SELECT DISTINCT topic FROM question_bank_items WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND part = #{part} AND is_deleted = 0 ORDER BY topic ASC")
    List<String> findDistinctTopicsBySeasonAndPart(
            @Param("seasonLabel") String seasonLabel, @Param("part") String part);

    @Select(
            "SELECT * FROM question_bank_items WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND topic = #{topic} AND is_deleted = 0 "
                    + "AND part IN ('PART2', 'PART3') "
                    + "ORDER BY CASE part WHEN 'PART2' THEN 0 WHEN 'PART3' THEN 1 END, sort_order ASC, id ASC")
    List<QuestionBankItem> findBySeasonLabelAndTopicOrderPart2ThenPart3(
            @Param("seasonLabel") String seasonLabel, @Param("topic") String topic);

    @Select(
            "<script>"
                    + "SELECT DISTINCT topic FROM question_bank_items WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND is_deleted = 0 "
                    + "AND part IN <foreach collection='parts' item='p' open='(' separator=',' close=')'>#{p}</foreach> "
                    + "ORDER BY topic ASC"
                    + "</script>")
    List<String> findDistinctTopicsBySeasonAndParts(
            @Param("seasonLabel") String seasonLabel, @Param("parts") List<String> parts);

    @Select(
            "SELECT DISTINCT TRIM(season_label) FROM question_bank_items WHERE is_deleted = 0 AND TRIM(season_label) <> ''")
    List<String> findDistinctSeasonLabels();

    @Select(
            "SELECT * FROM question_bank_items WHERE is_deleted = 0 AND LOWER(topic) LIKE CONCAT('%', LOWER(#{q}), '%')")
    List<QuestionBankItem> findByTopicContainingIgnoreCase(@Param("q") String q);

    @Select(
            "<script>"
                    + "SELECT * FROM question_bank_items WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND is_deleted = 0 "
                    + "AND part IN <foreach collection='parts' item='p' open='(' separator=',' close=')'>#{p}</foreach> "
                    + "ORDER BY part ASC, sort_order ASC"
                    + "</script>")
    List<QuestionBankItem> findBySeasonLabelAndPartsNotDeleted(
            @Param("seasonLabel") String seasonLabel, @Param("parts") List<String> parts);

    @Update(
            "UPDATE question_bank_items SET topic = #{topic}, question_text = #{questionText}, answer_text = #{answerText}, "
                    + "keywords_json = #{keywordsJson}, sort_order = #{sortOrder} WHERE id = #{id} AND is_deleted = 0")
    int updateFieldsById(QuestionBankItem item);

    @Update("UPDATE question_bank_items SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int softDeleteById(@Param("id") Long id);

    @Update(
            "UPDATE question_bank_items SET is_deleted = 1 WHERE TRIM(season_label) = TRIM(#{seasonLabel}) AND is_deleted = 0")
    int softDeleteBySeasonLabel(@Param("seasonLabel") String seasonLabel);
}
