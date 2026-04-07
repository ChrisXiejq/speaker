package com.speaker.app.service.markdown;

import com.speaker.app.model.entity.QuestionBankItem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理员粘贴的 Markdown 文本解析为题库行。
 *
 * <h3>Part 1</h3>
 * <pre>
 * ## Topic: Quiet / Noisy places
 *
 * ### Question
 * Do you like quiet or noisy places?
 *
 * ### Answer
 * I prefer quiet places...
 *
 * ### Keywords
 * - quiet environment
 * - noisy street
 * </pre>
 *
 * <h3>Part 2 &amp; 3</h3>
 * <pre>
 * ## Topic: 感兴趣的科学
 *
 * ### Part 2
 *
 * #### Cue card
 * Describe an area of science...
 *
 * #### Answer
 * ...
 *
 * #### Keywords
 * - astronomy
 *
 * ### Part 3
 *
 * #### Question
 * Why do some children not like science?
 *
 * #### Answer
 * ...
 *
 * #### Keywords
 * - abstract
 * </pre>
 */
public final class BankMarkdownParser {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s*(.*)$");

    private BankMarkdownParser() {
    }

    public static ParseResult parsePart1(String markdown) {
        List<String> warnings = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return new ParseResult(List.of(), warnings);
        }
        List<String> lines = normalizeLines(markdown);
        List<QuestionBankItem> items = new ArrayList<>();
        String topic = "Part 1";
        int i = 0;
        while (i < lines.size()) {
            Heading h = parseHeading(lines.get(i));
            if (h != null && h.level == 2) {
                topic = stripTopicTitle(h.title);
                i++;
                continue;
            }
            if (h != null && h.level == 3 && isQuestionHeading(h.title)) {
                i++;
                Block q = readUntilLevel(lines, i, 3);
                i = q.endIndex;
                SubBlock a = readLabeledSection(lines, i, 3, "answer", "a");
                if (a == null) {
                    warnings.add("Part1：缺少 ### Answer（topic=" + topic + "）");
                    continue;
                }
                i = a.endIndex;
                SubBlock k = readLabeledSection(lines, i, 3, "keywords", "关键词");
                i = k != null ? k.endIndex : i;
                items.add(QuestionBankItem.builder()
                        .part("PART1")
                        .topic(topic)
                        .questionText(q.text.trim())
                        .answerText(nullIfBlank(a.text))
                        .keywordsJson(keywordsToJson(k != null ? k.text : ""))
                        .build());
                continue;
            }
            i++;
        }
        if (items.isEmpty() && !lines.isEmpty()) {
            warnings.add("Part1：未解析到 ### Question，请检查格式");
        }
        return new ParseResult(items, warnings);
    }

    public static ParseResult parsePart2And3(String markdown) {
        List<String> warnings = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return new ParseResult(List.of(), warnings);
        }
        List<String> lines = normalizeLines(markdown);
        List<QuestionBankItem> items = new ArrayList<>();
        String topic = "Part 2 & 3";
        int i = 0;
        while (i < lines.size()) {
            Heading h = parseHeading(lines.get(i));
            if (h != null && h.level == 2) {
                topic = stripTopicTitle(h.title);
                i++;
                continue;
            }
            if (h != null && h.level == 3 && isPart2Heading(h.title)) {
                i++;
                SubBlock cue = readLabeledSection(lines, i, 4, "cue card", "cuecard");
                if (cue == null) {
                    warnings.add("Part2：缺少 #### Cue card（topic=" + topic + "）");
                    continue;
                }
                i = cue.endIndex;
                SubBlock ans = readLabeledSection(lines, i, 4, "answer", "a");
                if (ans == null) {
                    warnings.add("Part2：缺少 #### Answer（topic=" + topic + "）");
                    continue;
                }
                i = ans.endIndex;
                SubBlock kw = readLabeledSection(lines, i, 4, "keywords", "关键词");
                i = kw != null ? kw.endIndex : i;
                items.add(QuestionBankItem.builder()
                        .part("PART2")
                        .topic(topic)
                        .questionText(cue.text.trim())
                        .answerText(nullIfBlank(ans.text))
                        .keywordsJson(keywordsToJson(kw != null ? kw.text : ""))
                        .build());
                continue;
            }
            if (h != null && h.level == 3 && isPart3Heading(h.title)) {
                i++;
                while (i < lines.size()) {
                    Heading h2 = parseHeading(lines.get(i));
                    if (h2 != null && h2.level == 2) {
                        break;
                    }
                    if (h2 != null && h2.level == 3) {
                        break;
                    }
                    if (h2 != null && h2.level == 4 && isQuestionHeading(h2.title)) {
                        i++;
                        SubBlock q = readPlainUntilLevel(lines, i, 4);
                        i = q.endIndex;
                        SubBlock a = readLabeledSection(lines, i, 4, "answer", "a");
                        if (a == null) {
                            warnings.add("Part3：缺少 #### Answer（topic=" + topic + "）");
                            break;
                        }
                        i = a.endIndex;
                        SubBlock k = readLabeledSection(lines, i, 4, "keywords", "关键词");
                        i = k != null ? k.endIndex : i;
                        items.add(QuestionBankItem.builder()
                                .part("PART3")
                                .topic(topic)
                                .questionText(q.text.trim())
                                .answerText(nullIfBlank(a.text))
                                .keywordsJson(keywordsToJson(k != null ? k.text : ""))
                                .build());
                        continue;
                    }
                    i++;
                }
                continue;
            }
            if (h != null && h.level == 4 && isCueCardHeading(h.title)) {
                SubBlock cue = readPlainUntilLevel(lines, i + 1, 4);
                i = cue.endIndex;
                SubBlock ans = readLabeledSection(lines, i, 4, "answer", "a");
                if (ans == null) {
                    warnings.add("Part2：#### Cue card 后缺少 #### Answer（topic=" + topic + "）");
                    continue;
                }
                i = ans.endIndex;
                SubBlock kw = readLabeledSection(lines, i, 4, "keywords", "关键词");
                i = kw != null ? kw.endIndex : i;
                items.add(QuestionBankItem.builder()
                        .part("PART2")
                        .topic(topic)
                        .questionText(cue.text.trim())
                        .answerText(nullIfBlank(ans.text))
                        .keywordsJson(keywordsToJson(kw != null ? kw.text : ""))
                        .build());
                continue;
            }
            i++;
        }
        if (items.isEmpty() && !lines.isEmpty()) {
            warnings.add("Part2&3：未解析到内容，请使用 ### Part 2 / #### Cue card 等标题");
        }
        return new ParseResult(items, warnings);
    }

    private static SubBlock readLabeledSection(List<String> lines, int start, int labelLevel, String... labelAliases) {
        int i = start;
        while (i < lines.size()) {
            Heading h = parseHeading(lines.get(i));
            if (h != null && h.level == labelLevel && matchesLabel(h.title, labelAliases)) {
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < lines.size()) {
                    Heading nh = parseHeading(lines.get(i));
                    if (nh != null && nh.level <= labelLevel) {
                        break;
                    }
                    appendLine(sb, lines.get(i));
                    i++;
                }
                return new SubBlock(sb.toString(), i);
            }
            i++;
        }
        return null;
    }

    private static SubBlock readPlainUntilLevel(List<String> lines, int start, int stopAtLevel) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < lines.size()) {
            Heading nh = parseHeading(lines.get(i));
            if (nh != null && nh.level <= stopAtLevel) {
                break;
            }
            appendLine(sb, lines.get(i));
            i++;
        }
        return new SubBlock(sb.toString(), i);
    }

    private static Block readUntilLevel(List<String> lines, int start, int maxLevel) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < lines.size()) {
            Heading nh = parseHeading(lines.get(i));
            if (nh != null && nh.level <= maxLevel) {
                break;
            }
            appendLine(sb, lines.get(i));
            i++;
        }
        return new Block(sb.toString(), i);
    }

    private static String stripTopicTitle(String title) {
        return title.replaceFirst("(?i)^Topic:\\s*", "").trim();
    }

    private static boolean isQuestionHeading(String title) {
        String t = title.toLowerCase();
        return t.equals("question") || t.equals("q");
    }

    private static boolean isCueCardHeading(String title) {
        String t = title.toLowerCase().replace(" ", "");
        return t.equals("cuecard") || title.toLowerCase().startsWith("cue card");
    }

    private static boolean isPart2Heading(String title) {
        return title.equalsIgnoreCase("Part 2") || title.equalsIgnoreCase("Part2");
    }

    private static boolean isPart3Heading(String title) {
        return title.equalsIgnoreCase("Part 3") || title.equalsIgnoreCase("Part3");
    }

    private static boolean matchesLabel(String title, String... aliases) {
        String norm = title.toLowerCase().replace(" ", "").replace("：", ":");
        for (String a : aliases) {
            String aa = a.toLowerCase().replace(" ", "");
            if (norm.equals(aa) || norm.startsWith(aa + ":")) {
                return true;
            }
        }
        return false;
    }

    private static void appendLine(StringBuilder sb, String line) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(line);
    }

    private static Heading parseHeading(String line) {
        Matcher m = HEADING.matcher(line.trim());
        if (!m.matches()) {
            return null;
        }
        int level = m.group(1).length();
        String title = m.group(2).trim();
        return new Heading(level, title);
    }

    private static List<String> normalizeLines(String md) {
        String s = md.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>();
        for (String line : s.split("\n")) {
            lines.add(line);
        }
        return lines;
    }

    private static String nullIfBlank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String keywordsToJson(String keywordBlock) {
        List<String> items = new ArrayList<>();
        for (String line : keywordBlock.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("- ") || t.startsWith("* ") || t.startsWith("• ")) {
                items.add(t.substring(2).trim());
            } else if (t.matches("^\\d+\\.\\s+.+")) {
                items.add(t.replaceFirst("^\\d+\\.\\s+", "").trim());
            } else if (!t.startsWith("#")) {
                items.add(t);
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(escapeJson(items.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Heading(int level, String title) {
    }

    private record Block(String text, int endIndex) {
    }

    private record SubBlock(String text, int endIndex) {
    }

    public record ParseResult(List<QuestionBankItem> items, List<String> warnings) {
    }
}
