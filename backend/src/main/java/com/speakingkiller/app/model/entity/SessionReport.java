package com.speakingkiller.app.model.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionReport {

    private Long id;
    private Long sessionId;
    private Integer pronunciationScore;
    private Integer grammarScore;
    private Integer coherenceScore;
    private Integer fluencyScore;
    private Integer ideasScore;
    private String overallBand;
    private String detailedFeedback;
    private String suggestionsJson;
}
