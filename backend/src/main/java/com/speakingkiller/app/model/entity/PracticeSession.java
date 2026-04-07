package com.speakingkiller.app.model.entity;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeSession {

    public enum Part { PART1, PART2, PART3 }

    public enum Status { IN_PROGRESS, COMPLETED, ABORTED }

    private Long id;
    private Long userId;
    private Part part;
    private String topic;
    private String topicPrompt;
    private Status status;
    private Instant startedAt;
    private Instant endedAt;
}
