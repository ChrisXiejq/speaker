package com.speakingkiller.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakingkiller.app.config.AppProperties;
import com.speakingkiller.app.dto.PracticeReplyResponse;
import com.speakingkiller.app.dto.PracticeStartRequest;
import com.speakingkiller.app.dto.PracticeStartResponse;
import com.speakingkiller.app.mapper.*;
import com.speakingkiller.app.model.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PracticeService {

    private static final String EXAMINER_SYSTEM = """
            You are a professional IELTS speaking examiner from the UK.
            Use standard British English vocabulary and tone.
            Keep questions natural and aligned with the real IELTS Speaking test structure.
            """;

    private final QwenService qwenService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PracticeSessionMapper sessionMapper;
    private final ConversationTurnMapper turnMapper;
    private final SessionReportMapper reportMapper;
    private final QuestionBankItemMapper bankMapper;
    private final CurrentUserService currentUserService;

    public PracticeService(
            QwenService qwenService,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PracticeSessionMapper sessionMapper,
            ConversationTurnMapper turnMapper,
            SessionReportMapper reportMapper,
            QuestionBankItemMapper bankMapper,
            CurrentUserService currentUserService) {
        this.qwenService = qwenService;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.sessionMapper = sessionMapper;
        this.turnMapper = turnMapper;
        this.reportMapper = reportMapper;
        this.bankMapper = bankMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public PracticeStartResponse startSession(PracticeStartRequest req) {
        User user = currentUserService.requireCurrentUser();
        PracticeSession.Part part = PracticeSession.Part.valueOf(req.part());
        String season = req.season() != null && !req.season().isBlank()
                ? req.season()
                : appProperties.getSpeaking().getCurrentSeason();

        String topic;
        String topicPrompt;

        if ("CUSTOM".equalsIgnoreCase(req.topicSource())) {
            if (req.customTopic() == null || req.customTopic().isBlank()) {
                throw new IllegalArgumentException("自定义话题不能为空");
            }
            topic = req.customTopic().trim();
            if (part == PracticeSession.Part.PART2) {
                topicPrompt = qwenService.chat(EXAMINER_SYSTEM + """
                        The candidate chose a custom topic. Produce a short Part 2 style cue card
                        (bullet points) in English only, suitable for one minute preparation + two minutes speaking.
                        Topic: """ + topic, List.of());
            } else {
                topicPrompt = topic;
            }
        } else {
            QuestionBankItem item = resolveBankItem(season, part.name(), req.bankQuestionId());
            topic = item.getTopic();
            topicPrompt = item.getQuestionText();
        }

        PracticeSession session = PracticeSession.builder()
                .userId(user.getId())
                .part(part)
                .topic(topic)
                .topicPrompt(topicPrompt)
                .status(PracticeSession.Status.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();
        sessionMapper.insert(session);

        String userPrompt = buildOpeningUserPrompt(part, topic, topicPrompt);
        String examinerLine = qwenService.chat(EXAMINER_SYSTEM, List.of(Map.of("role", "user", "content", userPrompt)));

        int seq = 1;
        ConversationTurn t = ConversationTurn.builder()
                .sessionId(session.getId())
                .seq(seq)
                .role(ConversationTurn.Role.EXAMINER)
                .content(examinerLine.trim())
                .briefEval(null)
                .build();
        turnMapper.insert(t);

        return new PracticeStartResponse(session.getId(), examinerLine.trim(), part.name(), topic);
    }

    private QuestionBankItem resolveBankItem(String season, String partName, Long bankQuestionId) {
        List<QuestionBankItem> list = bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(season, partName);
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题库暂无该 Part 的题目，请先在后台导入或更换季节标签");
        }
        if (bankQuestionId != null) {
            QuestionBankItem item = bankMapper.findById(bankQuestionId);
            if (item == null) {
                throw new IllegalArgumentException("题目不存在");
            }
            if (!season.equals(item.getSeasonLabel()) || !partName.equals(item.getPart())) {
                throw new IllegalArgumentException("题目与季节或 Part 不匹配");
            }
            return item;
        }
        return list.get(new Random().nextInt(list.size()));
    }

    private static String buildOpeningUserPrompt(PracticeSession.Part part, String topic, String topicPrompt) {
        return switch (part) {
            case PART1 -> """
                    Start Part 1. Greet briefly as an examiner, then ask ONE first question about everyday life.
                    Topic area hint: %s
                    Question detail: %s
                    Output only the examiner's spoken lines (no meta, no bullet labels).
                    """.formatted(topic, topicPrompt);
            case PART2 -> """
                    Start Part 2. Give brief instructions for the long turn.
                    Then present the cue card clearly (the bullet content below).
                    After that, say you will give preparation time in a real test (here we simulate directly).
                    Output only the examiner's spoken lines.
                    Cue card:\n%s
                    """.formatted(topicPrompt);
            case PART3 -> """
                    Start Part 3. Ask ONE abstract/discussion question related to the theme.
                    Theme: %s
                    Anchor: %s
                    Output only the examiner's spoken lines.
                    """.formatted(topic, topicPrompt);
        };
    }

    @Transactional
    public PracticeReplyResponse reply(long sessionId, String userText) {
        User user = currentUserService.requireCurrentUser();
        PracticeSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!session.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该会话");
        }
        if (session.getStatus() != PracticeSession.Status.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会话已结束");
        }
        if (userText == null || userText.isBlank()) {
            throw new IllegalArgumentException("回答不能为空");
        }

        List<ConversationTurn> history = turnMapper.findBySessionIdOrderBySeqAsc(sessionId);
        int nextSeq = history.stream().mapToInt(ConversationTurn::getSeq).max().orElse(0) + 1;

        turnMapper.insert(ConversationTurn.builder()
                .sessionId(sessionId)
                .seq(nextSeq)
                .role(ConversationTurn.Role.USER)
                .content(userText.trim())
                .briefEval(null)
                .build());

        history = turnMapper.findBySessionIdOrderBySeqAsc(sessionId);

        String userPrompt = buildFollowUpPrompt(session, history);
        String raw = qwenService.chat(EXAMINER_SYSTEM + """
                After each candidate answer, respond with STRICT JSON only, no markdown, no extra text.
                Schema:
                {
                  "brief_eval": "1-2 sentences feedback on their last answer",
                  "examiner_line": "your next question or follow-up in British English",
                  "should_end": false
                }
                Set should_end to true if this part has enough exchanges for a realistic mock (Part1 ~4-6 turns total including opening; Part2 after follow-up; Part3 ~3-5 questions).
                """, List.of(Map.of("role", "user", "content", userPrompt)));

        JsonNode node = parseJsonLoose(raw);
        String briefEval = textOrEmpty(node, "brief_eval");
        String examinerLine = textOrEmpty(node, "examiner_line");
        boolean shouldEnd = node.path("should_end").asBoolean(false);

        int seq2 = nextSeq + 1;
        turnMapper.insert(ConversationTurn.builder()
                .sessionId(sessionId)
                .seq(seq2)
                .role(ConversationTurn.Role.EXAMINER)
                .content(examinerLine)
                .briefEval(briefEval)
                .build());

        return new PracticeReplyResponse(examinerLine, briefEval, shouldEnd);
    }

    private String buildFollowUpPrompt(PracticeSession session, List<ConversationTurn> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("Part: ").append(session.getPart()).append("\n");
        sb.append("Topic: ").append(session.getTopic()).append("\n");
        sb.append("Transcript so far:\n");
        for (ConversationTurn t : history) {
            sb.append(t.getRole() == ConversationTurn.Role.EXAMINER ? "Examiner: " : "Candidate: ");
            sb.append(t.getContent()).append("\n");
        }
        sb.append("Continue the test naturally.");
        return sb.toString();
    }

    private JsonNode parseJsonLoose(String raw) {
        String s = raw == null ? "" : raw.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return objectMapper.createObjectNode()
                    .put("brief_eval", "")
                    .put("examiner_line", s)
                    .put("should_end", false);
        }
        String json = s.substring(start, end + 1);
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode()
                    .put("brief_eval", "")
                    .put("examiner_line", s)
                    .put("should_end", false);
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText("");
    }

    @Transactional
    public SessionReport complete(long sessionId) {
        User user = currentUserService.requireCurrentUser();
        PracticeSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!session.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该会话");
        }
        List<ConversationTurn> history = turnMapper.findBySessionIdOrderBySeqAsc(sessionId);
        if (history.size() < 2) {
            throw new IllegalArgumentException("对话过短，无法生成完整评价");
        }

        String transcript = history.stream()
                .map(t -> (t.getRole() == ConversationTurn.Role.EXAMINER ? "Examiner: " : "Candidate: ")
                        + t.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = """
                Based on the IELTS Speaking rubric, score the candidate from 0-9 for:
                pronunciation, grammar, coherence, fluency, ideas (lexical resource & task response).
                Return STRICT JSON only:
                {
                  "pronunciation": 6,
                  "grammar": 6,
                  "coherence": 6,
                  "fluency": 6,
                  "ideas": 6,
                  "overall_band": "6.0",
                  "detailed_feedback": "multi-paragraph feedback in concise English",
                  "suggestions": ["...", "..."]
                }
                Transcript:
                """ + transcript;

        String raw = qwenService.chat(EXAMINER_SYSTEM, List.of(Map.of("role", "user", "content", prompt)));
        JsonNode node = parseJsonLoose(raw);

        SessionReport report = SessionReport.builder()
                .sessionId(sessionId)
                .pronunciationScore(node.path("pronunciation").asInt(0))
                .grammarScore(node.path("grammar").asInt(0))
                .coherenceScore(node.path("coherence").asInt(0))
                .fluencyScore(node.path("fluency").asInt(0))
                .ideasScore(node.path("ideas").asInt(0))
                .overallBand(node.path("overall_band").asText(""))
                .detailedFeedback(node.path("detailed_feedback").asText(""))
                .suggestionsJson(node.path("suggestions").toString())
                .build();

        SessionReport existing = reportMapper.findBySessionId(sessionId);
        if (existing != null) {
            report.setId(existing.getId());
            reportMapper.update(report);
        } else {
            reportMapper.insert(report);
        }

        session.setStatus(PracticeSession.Status.COMPLETED);
        session.setEndedAt(Instant.now());
        sessionMapper.update(session);

        SessionReport saved = reportMapper.findBySessionId(sessionId);
        if (saved == null) {
            throw new IllegalStateException("保存评分报告失败");
        }
        return saved;
    }

    public Page<PracticeSession> history(Pageable pageable) {
        User user = currentUserService.requireCurrentUser();
        long total = sessionMapper.countByUserId(user.getId());
        List<PracticeSession> list = sessionMapper.findByUserIdOrderByStartedAtDesc(
                user.getId(),
                pageable.getOffset(),
                pageable.getPageSize());
        return new PageImpl<>(list, pageable, total);
    }

    public SessionDetailDto getSessionDetail(long sessionId) {
        User user = currentUserService.requireCurrentUser();
        PracticeSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!session.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该会话");
        }
        List<ConversationTurn> turns = turnMapper.findBySessionIdOrderBySeqAsc(sessionId);
        SessionReport report = reportMapper.findBySessionId(sessionId);
        return new SessionDetailDto(session, turns, report);
    }

    public record SessionDetailDto(
            PracticeSession session,
            List<ConversationTurn> turns,
            SessionReport report
    ) {}
}
