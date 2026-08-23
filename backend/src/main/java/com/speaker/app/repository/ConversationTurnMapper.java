package com.speaker.app.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speaker.app.model.entity.ConversationTurn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConversationTurnMapper extends BaseMapper<ConversationTurn> {

    @Select("SELECT * FROM conversation_turns WHERE session_id = #{sessionId} ORDER BY seq ASC")
    List<ConversationTurn> findBySessionIdOrderBySeqAsc(@Param("sessionId") Long sessionId);
}
