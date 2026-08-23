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
@TableName("question_bank_items")
public class QuestionBankItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String seasonLabel;
    private String part;
    private String topic;
    private String questionText;
    /** 参考答案（答案版 PDF 解析或手工录入） */
    private String answerText;
    /** 关键词 JSON 数组字符串，如 ["quiet environment","noisy street"] */
    private String keywordsJson;
    private Integer sortOrder;
    /** 软删除：true 表示对用户侧不可见 */
    private Boolean isDeleted;
}
