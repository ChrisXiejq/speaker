package com.speaker.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 题库模式：同一话题下按 sort_order 顺序提问；阶段见 {@link #phase}。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankTopicPracticeState {

    public static final String PHASE_BANK = "BANK";
    public static final String PHASE_AI_EXPAND = "AI_EXPAND";
    public static final String PHASE_AWAIT_NEXT_TOPIC = "AWAIT_NEXT_TOPIC";

    private List<Long> bankQuestionIds = new ArrayList<>();
    /** 当前已展示到的题库问题下标（开场为 0，对应第一题） */
    private int lastPresentedBankIndex;
    private String phase = PHASE_BANK;

    public List<Long> getBankQuestionIds() {
        return bankQuestionIds;
    }

    public void setBankQuestionIds(List<Long> bankQuestionIds) {
        this.bankQuestionIds = bankQuestionIds != null ? bankQuestionIds : new ArrayList<>();
    }

    public int getLastPresentedBankIndex() {
        return lastPresentedBankIndex;
    }

    public void setLastPresentedBankIndex(int lastPresentedBankIndex) {
        this.lastPresentedBankIndex = lastPresentedBankIndex;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase != null ? phase : PHASE_BANK;
    }
}
