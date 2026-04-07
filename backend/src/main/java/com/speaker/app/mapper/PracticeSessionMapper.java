package com.speaker.app.mapper;

import com.speaker.app.model.entity.PracticeSession;
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

    /** 软删除当前用户的会话；非本人或已删则影响行数为 0 */
    int softDeleteByIdForUser(@Param("id") Long id, @Param("userId") Long userId);
}
