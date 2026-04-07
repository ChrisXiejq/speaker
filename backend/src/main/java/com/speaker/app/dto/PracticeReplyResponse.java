package com.speaker.app.dto;

public record PracticeReplyResponse(
        String examinerLine,
        String briefEval,
        boolean shouldEnd,
        String referenceAnswer,
        String keywordsJson,
        /** BANK / AI_EXPAND / AWAIT_NEXT_TOPIC / LEGACY */
        String practicePhase,
        /** 严格模式：本题话题库题已全部问完，前端应提示是否进入下一话题 */
        boolean strictTopicFinished,
        /** 扩展模式：可显示「进入下一话题」按钮 */
        boolean canAdvanceTopic,
        /** 当前考官问题是否为 AI 扩展题 */
        boolean aiExpandedQuestion
) {}
