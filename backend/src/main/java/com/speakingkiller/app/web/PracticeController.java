package com.speakingkiller.app.web;

import com.speakingkiller.app.dto.PracticeReplyRequest;
import com.speakingkiller.app.dto.PracticeReplyResponse;
import com.speakingkiller.app.dto.PracticeStartRequest;
import com.speakingkiller.app.dto.PracticeStartResponse;
import com.speakingkiller.app.model.entity.PracticeSession;
import com.speakingkiller.app.model.entity.SessionReport;
import com.speakingkiller.app.service.PracticeService;
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
}
