package com.speaker.app.service.markdown;

import com.speaker.app.model.entity.QuestionBankItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankMarkdownParserTest {

    @Test
    void part1ParsesQuestions() {
        String md = """
                ## Topic: Art

                ### Question
                Do you like art?

                ### Answer
                Yes, I like art.

                ### Keywords
                - beauty
                - emotions
                """;
        BankMarkdownParser.ParseResult r = BankMarkdownParser.parsePart1(md);
        assertEquals(1, r.items().size());
        QuestionBankItem q = r.items().get(0);
        assertEquals("PART1", q.getPart());
        assertEquals("Art", q.getTopic());
        assertTrue(q.getQuestionText().contains("Do you like art"));
        assertTrue(q.getAnswerText().contains("Yes"));
        assertNotNull(q.getKeywordsJson());
    }

    @Test
    void part2And3Parses() {
        String md = """
                ## Topic: Science

                ### Part 2

                #### Cue card
                Describe an area of science.

                #### Answer
                Astronomy fascinates me.

                #### Keywords
                - stars

                ### Part 3

                #### Question
                Why children dislike science?

                #### Answer
                Too abstract.

                #### Keywords
                - abstract
                """;
        BankMarkdownParser.ParseResult r = BankMarkdownParser.parsePart2And3(md);
        List<QuestionBankItem> items = r.items();
        assertEquals(2, items.size());
        assertEquals("PART2", items.get(0).getPart());
        assertEquals("PART3", items.get(1).getPart());
    }
}
