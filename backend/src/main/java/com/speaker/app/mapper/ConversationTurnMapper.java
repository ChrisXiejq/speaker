package com.speaker.app.mapper;

import com.speaker.app.model.entity.ConversationTurn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationTurnMapper {

    int insert(ConversationTurn turn);

    List<ConversationTurn> findBySessionIdOrderBySeqAsc(@Param("sessionId") Long sessionId);
}
