package com.speaker.app.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speaker.app.model.entity.SessionReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionReportMapper extends BaseMapper<SessionReport> {

    @Update(
            "UPDATE session_reports SET pronunciation_score = #{pronunciationScore}, grammar_score = #{grammarScore}, "
                    + "coherence_score = #{coherenceScore}, fluency_score = #{fluencyScore}, ideas_score = #{ideasScore}, "
                    + "overall_band = #{overallBand}, detailed_feedback = #{detailedFeedback}, suggestions_json = #{suggestionsJson} "
                    + "WHERE id = #{id}")
    int update(SessionReport report);

    @Select("SELECT * FROM session_reports WHERE session_id = #{sessionId}")
    SessionReport findBySessionId(@Param("sessionId") Long sessionId);
}
