package com.speakingkiller.app.model.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationTurn {

    public enum Role { EXAMINER, USER }

    private Long id;
    private Long sessionId;
    private int seq;
    private Role role;
    private String content;
    private String briefEval;
}
