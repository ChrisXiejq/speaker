package com.speaker.app.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("session_reports")
public class SessionReport {

    @TableId(type = IdType.AUTO)
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
