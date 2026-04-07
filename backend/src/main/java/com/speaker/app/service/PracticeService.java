package com.speaker.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speaker.app.dto.BankTopicPracticeState;
import com.speaker.app.dto.PracticeReplyResponse;
import com.speaker.app.dto.PracticeStartRequest;
import com.speaker.app.dto.PracticeStartResponse;
import com.speaker.app.mapper.ConversationTurnMapper;
import com.speaker.app.mapper.PracticeSessionMapper;
import com.speaker.app.mapper.QuestionBankItemMapper;
import com.speaker.app.mapper.SessionReportMapper;
import com.speaker.app.model.entity.*;
import com.speaker.app.speaking.SeasonLabelResolver;
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

    private static final String AI_EXPAND_MARKER = "(此问题为AI扩展)";

    private final QwenService qwenService;
    private final ObjectMapper objectMapper;
    private final PracticeSessionMapper sessionMapper;
    private final ConversationTurnMapper turnMapper;
    private final SessionReportMapper reportMapper;
    private final QuestionBankItemMapper bankMapper;
    private final CurrentUserService currentUserService;
    private final SeasonLabelResolver seasonLabelResolver;

    public PracticeService(
            QwenService qwenService,
            ObjectMapper objectMapper,
            PracticeSessionMapper sessionMapper,
            ConversationTurnMapper turnMapper,
            SessionReportMapper reportMapper,
            QuestionBankItemMapper bankMapper,
            CurrentUserService currentUserService,
            SeasonLabelResolver seasonLabelResolver) {
        this.qwenService = qwenService;
        this.objectMapper = objectMapper;
        this.sessionMapper = sessionMapper;
        this.turnMapper = turnMapper;
        this.reportMapper = reportMapper;
        this.bankMapper = bankMapper;
        this.currentUserService = currentUserService;
        this.seasonLabelResolver = seasonLabelResolver;
    }

    @Transactional
    public PracticeStartResponse startSession(PracticeStartRequest req) {
        User user = currentUserService.requireCurrentUser();
        PracticeSession.Part part = PracticeSession.Part.valueOf(req.part());
        boolean allowExpand = Boolean.TRUE.equals(req.allowAiExpand());
        String season = req.season() != null && !req.season().isBlank() ? req.season().trim() : null;
        if (!"CUSTOM".equalsIgnoreCase(req.topicSource())) {
            if (season == null) {
                season = seasonLabelResolver
                        .pickForNow(bankMapper.findDistinctSeasonLabels())
                        .orElseThrow(() -> new IllegalArgumentException("题库暂无可用季节，请先导入当季题目"));
            }
        }

        if ("CUSTOM".equalsIgnoreCase(req.topicSource())) {
            if (req.customTopic() == null || req.customTopic().isBlank()) {
                throw new IllegalArgumentException("自定义话题不能为空");
            }
            String topic = req.customTopic().trim();
            String topicPrompt;
            if (part == PracticeSession.Part.PART2 || part == PracticeSession.Part.PART2_AND_3) {
                topicPrompt = qwenService.chat(EXAMINER_SYSTEM + """
                        The candidate chose a custom topic. Produce a short Part 2 style cue card
                        (bullet points) in English only, suitable for one minute preparation + two minutes speaking.
                        Topic: """ + topic, List.of());
            } else {
                topicPrompt = topic;
            }
            PracticeSession session = PracticeSession.builder()
                    .userId(user.getId())
                    .part(part)
                    .topic(topic)
                    .topicPrompt(topicPrompt)
                    .topicSource("CUSTOM")
                    .seasonLabel(null)
                    .allowAiExpand(false)
                    .sessionStateJson(null)
                    .status(PracticeSession.Status.IN_PROGRESS)
                    .startedAt(Instant.now())
                    .build();
            sessionMapper.insert(session);

            String userPrompt = buildOpeningUserPrompt(part, topic, topicPrompt);
            String examinerLine = qwenService.chat(EXAMINER_SYSTEM, List.of(Map.of("role", "user", "content", userPrompt)));

            insertExaminerTurn(session.getId(), 1, examinerLine.trim());

            return new PracticeStartResponse(
                    session.getId(),
                    examinerLine.trim(),
                    part.name(),
                    topic,
                    null,
                    null,
                    false,
                    "LEGACY");
        }

        // BANK
        QuestionBankItem seed = resolveBankItem(season, part, req.bankQuestionId());
        List<QuestionBankItem> topicItems = loadTopicQueueForBank(season, part, seed.getTopic());
        if (topicItems.isEmpty()) {
            topicItems = List.of(seed);
        }

        List<Long> ids = topicItems.stream().map(QuestionBankItem::getId).toList();
        BankTopicPracticeState state = new BankTopicPracticeState();
        state.setBankQuestionIds(new ArrayList<>(ids));
        state.setLastPresentedBankIndex(0);
        state.setPhase(BankTopicPracticeState.PHASE_BANK);

        QuestionBankItem first = topicItems.get(0);
        String examinerLine = buildBankOpeningExaminerLine(part, first);
        String topic = first.getTopic();

        PracticeSession session = PracticeSession.builder()
                .userId(user.getId())
                .part(part)
                .topic(topic)
                .topicPrompt(first.getQuestionText())
                .topicSource("BANK")
                .seasonLabel(season)
                .allowAiExpand(allowExpand)
                .sessionStateJson(writeState(state))
                .status(PracticeSession.Status.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();
        sessionMapper.insert(session);

        insertExaminerTurn(session.getId(), 1, examinerLine);

        return new PracticeStartResponse(
                session.getId(),
                examinerLine,
                part.name(),
                topic,
                first.getAnswerText(),
                first.getKeywordsJson(),
                true,
                BankTopicPracticeState.PHASE_BANK);
    }

    @Transactional
    public PracticeStartResponse advanceToNextTopic(long sessionId) {
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
        if (!"BANK".equalsIgnoreCase(session.getTopicSource())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅题库顺序模式可切换话题");
        }
        BankTopicPracticeState state = readState(session);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会话状态异常");
        }
        if (!BankTopicPracticeState.PHASE_AWAIT_NEXT_TOPIC.equals(state.getPhase())
                && !BankTopicPracticeState.PHASE_AI_EXPAND.equals(state.getPhase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前阶段不可切换话题");
        }

        List<String> topics = session.getPart() == PracticeSession.Part.PART2_AND_3
                ? bankMapper.findDistinctTopicsBySeasonAndParts(
                        session.getSeasonLabel(), List.of("PART2", "PART3"))
                : bankMapper.findDistinctTopicsBySeasonAndPart(
                        session.getSeasonLabel(), session.getPart().name());
        topics.removeIf(t -> session.getTopic() != null && session.getTopic().equals(t));
        if (topics.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有其他可用话题");
        }
        Collections.shuffle(topics);
        String newTopic = topics.get(0);

        List<QuestionBankItem> topicItems =
                loadTopicQueueForBank(session.getSeasonLabel(), session.getPart(), newTopic);
        if (topicItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题库数据异常");
        }
        List<Long> ids = topicItems.stream().map(QuestionBankItem::getId).toList();
        state.setBankQuestionIds(new ArrayList<>(ids));
        state.setLastPresentedBankIndex(0);
        state.setPhase(BankTopicPracticeState.PHASE_BANK);
        session.setTopic(newTopic);
        session.setTopicPrompt(topicItems.get(0).getQuestionText());
        session.setSessionStateJson(writeState(state));
        sessionMapper.update(session);

        QuestionBankItem first = topicItems.get(0);
        String examinerLine = buildBankOpeningExaminerLine(session.getPart(), first);

        List<ConversationTurn> history = turnMapper.findBySessionIdOrderBySeqAsc(sessionId);
        int nextSeq = history.stream().mapToInt(ConversationTurn::getSeq).max().orElse(0) + 1;
        insertExaminerTurn(sessionId, nextSeq, examinerLine);

        return new PracticeStartResponse(
                session.getId(),
                examinerLine,
                session.getPart().name(),
                newTopic,
                first.getAnswerText(),
                first.getKeywordsJson(),
                true,
                BankTopicPracticeState.PHASE_BANK);
    }

    private static String buildBankOpeningExaminerLine(PracticeSession.Part part, QuestionBankItem item) {
        String q = item.getQuestionText() == null ? "" : item.getQuestionText().trim();
        if (part == PracticeSession.Part.PART2_AND_3) {
            if ("PART2".equals(item.getPart())) {
                return "Let's move to Part 2. Here is your cue card. In the real test you would have one minute to prepare.\n\n"
                        + q;
            }
            return "Now let's move to Part 3. I'd like to ask you some more questions related to this topic.\n\n" + q;
        }
        return switch (part) {
            case PART1 -> q;
            case PART2 -> "Let's move to Part 2. Here is your cue card. In the real test you would have one minute to prepare.\n\n"
                    + q;
            case PART3 -> q;
            case PART2_AND_3 -> q;
        };
    }

    private void insertExaminerTurn(long sessionId, int seq, String content) {
        turnMapper.insert(ConversationTurn.builder()
                .sessionId(sessionId)
                .seq(seq)
                .role(ConversationTurn.Role.EXAMINER)
                .content(content)
                .briefEval(null)
                .build());
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

        if ("BANK".equalsIgnoreCase(session.getTopicSource()) && session.getSessionStateJson() != null) {
            return replyBankTopicFlow(session, userText.trim());
        }
        return replyLegacy(session, userText.trim());
    }

    private PracticeReplyResponse replyBankTopicFlow(PracticeSession session, String userText) {
        BankTopicPracticeState state = readState(session);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会话状态异常，请重新开始练习");
        }

        if (BankTopicPracticeState.PHASE_AWAIT_NEXT_TOPIC.equals(state.getPhase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "本题话题库题已结束，请点击「进入下一话题」或结束练习");
        }

        List<Long> ids = state.getBankQuestionIds();
        int n = ids.size();
        int last = state.getLastPresentedBankIndex();

        List<ConversationTurn> history = turnMapper.findBySessionIdOrderBySeqAsc(session.getId());
        int nextSeq = history.stream().mapToInt(ConversationTurn::getSeq).max().orElse(0) + 1;

        turnMapper.insert(ConversationTurn.builder()
                .sessionId(session.getId())
                .seq(nextSeq)
                .role(ConversationTurn.Role.USER)
                .content(userText)
                .briefEval(null)
                .build());

        history = turnMapper.findBySessionIdOrderBySeqAsc(session.getId());

        if (BankTopicPracticeState.PHASE_BANK.equals(state.getPhase())) {
            if (last < n - 1) {
                state.setLastPresentedBankIndex(last + 1);
                session.setSessionStateJson(writeState(state));
                sessionMapper.update(session);

                int idx = state.getLastPresentedBankIndex();
                long id = ids.get(idx);
                QuestionBankItem item = requireBankItem(id);
                String examinerQ = formatBankExaminerQuestion(session, ids, idx, item);
                String briefEval = briefEvalOnly(history);
                int seq2 = nextSeq + 1;
                turnMapper.insert(ConversationTurn.builder()
                        .sessionId(session.getId())
                        .seq(seq2)
                        .role(ConversationTurn.Role.EXAMINER)
                        .content(examinerQ)
                        .briefEval(briefEval)
                        .build());

                return new PracticeReplyResponse(
                        examinerQ,
                        briefEval,
                        false,
                        item.getAnswerText(),
                        item.getKeywordsJson(),
                        BankTopicPracticeState.PHASE_BANK,
                        false,
                        false,
                        false);
            }

            // 刚答完本话题最后一题题库
            boolean allowExpand = Boolean.TRUE.equals(session.getAllowAiExpand());
            if (!allowExpand) {
                state.setPhase(BankTopicPracticeState.PHASE_AWAIT_NEXT_TOPIC);
                session.setSessionStateJson(writeState(state));
                sessionMapper.update(session);

                String briefEval = briefEvalOnly(history);
                String closing =
                        "Thank you. That completes all questions for this topic area.";
                int seq2 = nextSeq + 1;
                turnMapper.insert(ConversationTurn.builder()
                        .sessionId(session.getId())
                        .seq(seq2)
                        .role(ConversationTurn.Role.EXAMINER)
                        .content(closing)
                        .briefEval(briefEval)
                        .build());

                return new PracticeReplyResponse(
                        closing,
                        briefEval,
                        false,
                        null,
                        null,
                        BankTopicPracticeState.PHASE_AWAIT_NEXT_TOPIC,
                        true,
                        false,
                        false);
            }

            state.setPhase(BankTopicPracticeState.PHASE_AI_EXPAND);
            session.setSessionStateJson(writeState(state));
            sessionMapper.update(session);

            String aiLine = generateAiExpandQuestion(session, history);
            String briefEval = briefEvalOnly(history);
            int seq2 = nextSeq + 1;
            turnMapper.insert(ConversationTurn.builder()
                    .sessionId(session.getId())
                    .seq(seq2)
                    .role(ConversationTurn.Role.EXAMINER)
                    .content(aiLine)
                    .briefEval(briefEval)
                    .build());

            return new PracticeReplyResponse(
                    aiLine,
                    briefEval,
                    false,
                    null,
                    null,
                    BankTopicPracticeState.PHASE_AI_EXPAND,
                    false,
                    true,
                    true);
        }

        if (BankTopicPracticeState.PHASE_AI_EXPAND.equals(state.getPhase())) {
            String raw = qwenService.chat(EXAMINER_SYSTEM + """
                    After each candidate answer, respond with STRICT JSON only, no markdown, no extra text.
                    Schema:
                    {
                      "brief_eval": "1-2 sentences feedback on their last answer",
                      "examiner_line": "your next question in British English",
                      "should_end": false
                    }
                    You are in Part %s. Ask ONE follow-up question in British English.
                    End examiner_line with the exact Chinese suffix: %s
                    Set should_end to false.
                    """.formatted(session.getPart().name(), AI_EXPAND_MARKER),
                    List.of(Map.of("role", "user", "content", buildFollowUpPrompt(session, history))));

            JsonNode node = parseJsonLoose(raw);
            String briefEval = textOrEmpty(node, "brief_eval");
            String examinerLine = textOrEmpty(node, "examiner_line");
            boolean shouldEnd = node.path("should_end").asBoolean(false);

            int seq2 = nextSeq + 1;
            turnMapper.insert(ConversationTurn.builder()
                    .sessionId(session.getId())
                    .seq(seq2)
                    .role(ConversationTurn.Role.EXAMINER)
                    .content(examinerLine)
                    .briefEval(briefEval)
                    .build());

            return new PracticeReplyResponse(
                    examinerLine,
                    briefEval,
                    shouldEnd,
                    null,
                    null,
                    BankTopicPracticeState.PHASE_AI_EXPAND,
                    false,
                    true,
                    true);
        }

        throw new IllegalStateException("未处理的题库会话阶段");
    }

    private PracticeReplyResponse replyLegacy(PracticeSession session, String userText) {
        List<ConversationTurn> history = turnMapper.findBySessionIdOrderBySeqAsc(session.getId());
        int nextSeq = history.stream().mapToInt(ConversationTurn::getSeq).max().orElse(0) + 1;

        turnMapper.insert(ConversationTurn.builder()
                .sessionId(session.getId())
                .seq(nextSeq)
                .role(ConversationTurn.Role.USER)
                .content(userText)
                .briefEval(null)
                .build());

        history = turnMapper.findBySessionIdOrderBySeqAsc(session.getId());

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
                .sessionId(session.getId())
                .seq(seq2)
                .role(ConversationTurn.Role.EXAMINER)
                .content(examinerLine)
                .briefEval(briefEval)
                .build());

        return new PracticeReplyResponse(
                examinerLine,
                briefEval,
                shouldEnd,
                null,
                null,
                "LEGACY",
                false,
                false,
                false);
    }

    private String generateAiExpandQuestion(PracticeSession session, List<ConversationTurn> history) {
        String raw = qwenService.chat(EXAMINER_SYSTEM + """
                Respond with STRICT JSON only, no markdown, no extra text.
                Schema:
                {
                  "brief_eval": "",
                  "examiner_line": "ONE follow-up question in British English",
                  "should_end": false
                }
                Set brief_eval to empty string.
                You have finished all prepared questions for this topic. Ask ONE additional discussion question.
                End examiner_line with the exact Chinese suffix: %s
                """.formatted(AI_EXPAND_MARKER),
                List.of(Map.of("role", "user", "content", buildFollowUpPrompt(session, history))));

        JsonNode node = parseJsonLoose(raw);
        return textOrEmpty(node, "examiner_line");
    }

    private String briefEvalOnly(List<ConversationTurn> history) {
        StringBuilder sb = new StringBuilder();
        for (ConversationTurn t : history) {
            sb.append(t.getRole() == ConversationTurn.Role.EXAMINER ? "Examiner: " : "Candidate: ");
            sb.append(t.getContent()).append("\n");
        }
        String raw = qwenService.chat(EXAMINER_SYSTEM + """
                The candidate just answered. Reply with STRICT JSON only:
                {"brief_eval":"1-2 sentences of feedback in English on their last answer only"}
                """, List.of(Map.of("role", "user", "content", sb.toString())));
        return textOrEmpty(parseJsonLoose(raw), "brief_eval");
    }

    /**
     * 在 Part2&3 合并模式下，从 Part2 第一题切到 Part3 第一题时，在题目前加一句过渡。
     */
    private String formatBankExaminerQuestion(
            PracticeSession session, List<Long> ids, int index, QuestionBankItem item) {
        String q = item.getQuestionText() == null ? "" : item.getQuestionText().trim();
        if (session.getPart() != PracticeSession.Part.PART2_AND_3) {
            return q;
        }
        if (!"PART3".equals(item.getPart()) || index <= 0) {
            return q;
        }
        QuestionBankItem prev = requireBankItem(ids.get(index - 1));
        if ("PART2".equals(prev.getPart())) {
            return "Now let's move to Part 3. " + q;
        }
        return q;
    }

    private QuestionBankItem requireBankItem(long id) {
        QuestionBankItem item = bankMapper.findById(id);
        if (item == null || Boolean.TRUE.equals(item.getIsDeleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题库题目不存在或已删除");
        }
        return item;
    }

    private BankTopicPracticeState readState(PracticeSession session) {
        try {
            if (session.getSessionStateJson() == null || session.getSessionStateJson().isBlank()) {
                return null;
            }
            return objectMapper.readValue(session.getSessionStateJson(), BankTopicPracticeState.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String writeState(BankTopicPracticeState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            throw new IllegalStateException("保存会话状态失败");
        }
    }

    private List<QuestionBankItem> poolForRandomPick(String season, PracticeSession.Part part) {
        if (part == PracticeSession.Part.PART2_AND_3) {
            List<QuestionBankItem> all = new ArrayList<>();
            all.addAll(bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(season, "PART2"));
            all.addAll(bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(season, "PART3"));
            return all;
        }
        return bankMapper.findBySeasonLabelAndPartOrderBySortOrderAsc(season, part.name());
    }

    private List<QuestionBankItem> loadTopicQueueForBank(String season, PracticeSession.Part part, String topic) {
        if (part == PracticeSession.Part.PART2_AND_3) {
            return bankMapper.findBySeasonLabelAndTopicOrderPart2ThenPart3(season, topic);
        }
        return bankMapper.findBySeasonPartTopicOrderBySortOrderAsc(season, part.name(), topic);
    }

    private QuestionBankItem resolveBankItem(String season, PracticeSession.Part part, Long bankQuestionId) {
        List<QuestionBankItem> list = poolForRandomPick(season, part);
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题库暂无该 Part 的题目，请先在后台导入或更换季节标签");
        }
        if (bankQuestionId != null) {
            QuestionBankItem item = bankMapper.findById(bankQuestionId);
            if (item == null) {
                throw new IllegalArgumentException("题目不存在");
            }
            if (Boolean.TRUE.equals(item.getIsDeleted())) {
                throw new IllegalArgumentException("题目已删除");
            }
            if (!season.equals(item.getSeasonLabel())) {
                throw new IllegalArgumentException("题目与季节不匹配");
            }
            if (part == PracticeSession.Part.PART2_AND_3) {
                if (!"PART2".equals(item.getPart()) && !"PART3".equals(item.getPart())) {
                    throw new IllegalArgumentException("题目与 Part 2&3 模式不匹配");
                }
            } else if (!part.name().equals(item.getPart())) {
                throw new IllegalArgumentException("题目与 Part 不匹配");
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
            case PART2, PART2_AND_3 -> """
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

    private String buildFollowUpPrompt(PracticeSession session, List<ConversationTurn> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("Part: ").append(session.getPart()).append("\n");
        if (session.getPart() == PracticeSession.Part.PART2_AND_3) {
            sb.append(
                    "This section combines Part 2 (long turn) and Part 3 (discussion). Part 3 questions should be abstract, about society or real-world issues linked to the Part 2 theme.\n");
        }
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

    @Transactional
    public void softDeleteSession(long sessionId) {
        User user = currentUserService.requireCurrentUser();
        int n = sessionMapper.softDeleteByIdForUser(sessionId, user.getId());
        if (n == 0) {
            throw new IllegalArgumentException("会话不存在");
        }
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
