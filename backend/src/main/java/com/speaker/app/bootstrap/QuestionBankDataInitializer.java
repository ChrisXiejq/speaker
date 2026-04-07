package com.speaker.app.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speaker.app.mapper.QuestionBankItemMapper;
import com.speaker.app.model.entity.QuestionBankItem;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class QuestionBankDataInitializer implements CommandLineRunner {

    private final QuestionBankItemMapper bankMapper;
    private final ObjectMapper objectMapper;

    public QuestionBankDataInitializer(QuestionBankItemMapper bankMapper, ObjectMapper objectMapper) {
        this.bankMapper = bankMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (bankMapper.count() > 0) {
            return;
        }
        ClassPathResource res = new ClassPathResource("data/sample-questions.json");
        if (!res.exists()) {
            return;
        }
        try (InputStream in = res.getInputStream()) {
            List<Map<String, Object>> rows = objectMapper.readValue(in, new TypeReference<>() {});
            for (Map<String, Object> row : rows) {
                bankMapper.insert(rowToItem(row));
            }
        }
    }

    private QuestionBankItem rowToItem(Map<String, Object> row) {
        Object so = row.get("sortOrder");
        Integer sortOrder = so instanceof Number ? ((Number) so).intValue() : null;
        return QuestionBankItem.builder()
                .seasonLabel((String) row.get("seasonLabel"))
                .part((String) row.get("part"))
                .topic((String) row.get("topic"))
                .questionText((String) row.get("questionText"))
                .answerText(row.get("answerText") != null ? (String) row.get("answerText") : null)
                .keywordsJson(row.get("keywordsJson") != null ? (String) row.get("keywordsJson") : null)
                .sortOrder(sortOrder)
                .build();
    }
}
