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
@TableName("conversation_turns")
public class ConversationTurn {

    public enum Role { EXAMINER, USER }

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private int seq;
    private Role role;
    private String content;
    private String briefEval;
}
