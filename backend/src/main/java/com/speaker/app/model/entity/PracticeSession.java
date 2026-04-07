package com.speaker.app.model.entity;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeSession {

    /** PART2_AND_3：同一话题下先 Part 2 后 Part 3；历史会话可能仍为 PART2 / PART3 */
    public enum Part { PART1, PART2, PART3, PART2_AND_3 }

    public enum Status { IN_PROGRESS, COMPLETED, ABORTED }

    private Long id;
    private Long userId;
    private Part part;
    private String topic;
    private String topicPrompt;
    /** BANK / CUSTOM */
    private String topicSource;
    /** 题库模式下的季节标签 */
    private String seasonLabel;
    private Boolean allowAiExpand;
    /** JSON：BankTopicPracticeState */
    private String sessionStateJson;
    private Status status;
    private Instant startedAt;
    private Instant endedAt;
    /** 软删除：仅列表/详情不展示，数据保留 */
    @Builder.Default
    private Boolean isDeleted = false;
}
