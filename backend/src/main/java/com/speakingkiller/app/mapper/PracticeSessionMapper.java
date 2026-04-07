package com.speakingkiller.app.mapper;

import com.speakingkiller.app.model.entity.PracticeSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PracticeSessionMapper {

    int insert(PracticeSession session);

    int update(PracticeSession session);

    PracticeSession findById(@Param("id") Long id);

    long countByUserId(@Param("userId") Long userId);

    List<PracticeSession> findByUserIdOrderByStartedAtDesc(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("limit") int limit);
}
