package com.speakingkiller.app.model.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionBankItem {

    private Long id;
    private String seasonLabel;
    private String part;
    private String topic;
    private String questionText;
    private Integer sortOrder;
}
