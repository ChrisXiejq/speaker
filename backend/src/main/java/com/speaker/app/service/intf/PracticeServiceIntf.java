package com.speaker.app.service.intf;

import com.speaker.app.dto.PracticeReplyResponse;
import com.speaker.app.dto.PracticeStartRequest;
import com.speaker.app.dto.PracticeStartResponse;
import com.speaker.app.model.entity.ConversationTurn;
import com.speaker.app.model.entity.PracticeSession;
import com.speaker.app.model.entity.SessionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PracticeServiceIntf {

    PracticeStartResponse startSession(PracticeStartRequest req);

    PracticeStartResponse advanceToNextTopic(long sessionId);

    PracticeReplyResponse reply(long sessionId, String userText);

    SessionReport complete(long sessionId);

    void softDeleteSession(long sessionId);

    Page<PracticeSession> history(Pageable pageable);

    SessionDetailDto getSessionDetail(long sessionId);

    record SessionDetailDto(
            PracticeSession session,
            List<ConversationTurn> turns,
            SessionReport report
    ) {}
}
