package com.speakingkiller.app.mapper;

import com.speakingkiller.app.model.entity.SessionReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SessionReportMapper {

    int insert(SessionReport report);

    int update(SessionReport report);

    SessionReport findBySessionId(@Param("sessionId") Long sessionId);
}
