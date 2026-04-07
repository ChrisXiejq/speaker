package com.speaker.app.web;

import com.speaker.app.dto.PracticeReplyRequest;
import com.speaker.app.dto.PracticeReplyResponse;
import com.speaker.app.dto.PracticeStartRequest;
import com.speaker.app.dto.PracticeStartResponse;
import com.speaker.app.model.entity.PracticeSession;
import com.speaker.app.model.entity.SessionReport;
import com.speaker.app.service.PracticeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/sessions")
    public PracticeStartResponse start(@Valid @RequestBody PracticeStartRequest req) {
        return practiceService.startSession(req);
    }

    @PostMapping("/sessions/{id}/reply")
    public PracticeReplyResponse reply(
            @PathVariable("id") long id,
            @Valid @RequestBody PracticeReplyRequest req) {
        return practiceService.reply(id, req.userText());
    }

    /** 题库模式：进入下一话题（严格模式题库结束后，或 AI 扩展模式下随时可切换） */
    @PostMapping("/sessions/{id}/next-topic")
    public PracticeStartResponse nextTopic(@PathVariable("id") long id) {
        return practiceService.advanceToNextTopic(id);
    }

    @PostMapping("/sessions/{id}/complete")
    public SessionReport complete(@PathVariable("id") long id) {
        return practiceService.complete(id);
    }

    @GetMapping("/sessions/{id}")
    public PracticeService.SessionDetailDto detail(@PathVariable("id") long id) {
        return practiceService.getSessionDetail(id);
    }

    @GetMapping("/sessions")
    public Page<PracticeSession> history(@PageableDefault(size = 20) Pageable pageable) {
        return practiceService.history(pageable);
    }

    @DeleteMapping("/sessions/{id}")
    public void deleteSession(@PathVariable("id") long id) {
        practiceService.softDeleteSession(id);
    }
}
