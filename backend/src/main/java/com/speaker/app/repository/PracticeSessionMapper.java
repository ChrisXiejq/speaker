package com.speaker.app.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speaker.app.dto.PartAggregateRow;
import com.speaker.app.dto.SessionTrendRow;
import com.speaker.app.model.entity.PracticeSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PracticeSessionMapper extends BaseMapper<PracticeSession> {

    @Select("SELECT * FROM practice_sessions WHERE id = #{id} AND is_deleted = 0")
    PracticeSession findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM practice_sessions WHERE user_id = #{userId} AND is_deleted = 0")
    long countByUserId(@Param("userId") Long userId);

    @Select(
            "SELECT * FROM practice_sessions WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY started_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<PracticeSession> findByUserIdOrderByStartedAtDesc(
            @Param("userId") Long userId, @Param("offset") long offset, @Param("limit") int limit);

    @Update(
            "UPDATE practice_sessions SET is_deleted = 1 WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    int softDeleteByIdForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Update(
            "UPDATE practice_sessions SET user_id = #{userId}, part = #{part}, topic = #{topic}, topic_prompt = #{topicPrompt}, "
                    + "topic_source = #{topicSource}, season_label = #{seasonLabel}, allow_ai_expand = #{allowAiExpand}, "
                    + "session_state_json = #{sessionStateJson}, status = #{status}, started_at = #{startedAt}, ended_at = #{endedAt}, "
                    + "is_deleted = #{isDeleted} WHERE id = #{id} AND is_deleted = 0")
    int update(PracticeSession session);

    /** 已完成且有报告的会话数（用于统计） */
    @Select(
            "SELECT COUNT(*) FROM practice_sessions ps INNER JOIN session_reports sr ON sr.session_id = ps.id "
                    + "WHERE ps.user_id = #{userId} AND ps.is_deleted = 0 AND ps.status = 'COMPLETED'")
    long countCompletedWithReport(@Param("userId") long userId);

    /** 去重话题数（仅统计有非空 topic 的已完成会话） */
    @Select(
            "SELECT COUNT(DISTINCT ps.topic) FROM practice_sessions ps INNER JOIN session_reports sr ON sr.session_id = ps.id "
                    + "WHERE ps.user_id = #{userId} AND ps.is_deleted = 0 AND ps.status = 'COMPLETED' "
                    + "AND ps.topic IS NOT NULL AND LENGTH(TRIM(ps.topic)) > 0")
    long countDistinctTopicsCompleted(@Param("userId") long userId);

    /** 全部已完成会话的 Overall 均分（overall_band 需为数字串） */
    @Select(
            "SELECT AVG(CAST(TRIM(sr.overall_band) AS DECIMAL(4,2))) FROM practice_sessions ps "
                    + "INNER JOIN session_reports sr ON sr.session_id = ps.id "
                    + "WHERE ps.user_id = #{userId} AND ps.is_deleted = 0 AND ps.status = 'COMPLETED' "
                    + "AND sr.overall_band IS NOT NULL AND TRIM(sr.overall_band) <> '' "
                    + "AND sr.overall_band REGEXP '^[0-9]+(\\\\.[0-9])?$'")
    Double avgOverallBandAll(@Param("userId") long userId);

    @Select(
            "SELECT ps.part AS part, COUNT(*) AS sessionCount, "
                    + "AVG(CASE WHEN sr.overall_band REGEXP '^[0-9]+(\\\\.[0-9])?$' THEN CAST(TRIM(sr.overall_band) AS DECIMAL(4,2)) END) AS avgOverallBand, "
                    + "AVG(sr.pronunciation_score) AS avgPronunciation, AVG(sr.grammar_score) AS avgGrammar, "
                    + "AVG(sr.coherence_score) AS avgCoherence, AVG(sr.fluency_score) AS avgFluency, AVG(sr.ideas_score) AS avgIdeas "
                    + "FROM practice_sessions ps INNER JOIN session_reports sr ON sr.session_id = ps.id "
                    + "WHERE ps.user_id = #{userId} AND ps.is_deleted = 0 AND ps.status = 'COMPLETED' "
                    + "GROUP BY ps.part")
    List<PartAggregateRow> aggregateCompletedByPart(@Param("userId") long userId);

    /** 最近若干条已完成会话（按时间倒序），用于走势；前端再按时间正序画折线 */
    @Select(
            "SELECT ps.started_at AS startedAt, sr.overall_band AS overallBand, ps.part AS part FROM practice_sessions ps "
                    + "INNER JOIN session_reports sr ON sr.session_id = ps.id "
                    + "WHERE ps.user_id = #{userId} AND ps.is_deleted = 0 AND ps.status = 'COMPLETED' "
                    + "ORDER BY ps.started_at DESC LIMIT #{limit}")
    List<SessionTrendRow> recentCompletedTrend(@Param("userId") long userId, @Param("limit") int limit);
}
